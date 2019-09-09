/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.jcr;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventJournal;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.jackrabbit.commons.webdav.AtomFeedConstants;
import org.apache.jackrabbit.commons.webdav.EventUtil;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.AdditionalEventInfo;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Implements a JCR {@link EventJournal} in terms of an RFC 4287 Atom feed.
 * <p>
 * Each feed entry represents either a single event, or, if the repository
 * supports the {@link Event#PERSIST} event, an event bundle. The actual event
 * data is sent in the Atom &lt;content&gt; element and uses the same XML
 * serialization as the one used for subscriptions.
 * <p>
 * Skipping is implemented by specifying the desired time offset (represented
 * as hexadecimal long in ms since the epoch) disguised as ETag in the HTTP "If-None-Match" 
 * header field.
 * <p>
 * The generated feed may not be complete; the total number of events is limited in
 * order not to overload the client.
 * <p>
 * Furthermore, the number of events is limited by going up to 2000 ms into the future
 * (based on the request time). This is supposed to limit the wait time for the client).
 */
public class EventJournalResourceImpl extends AbstractResource {

    public static final String RELURIFROMWORKSPACE = "?type=journal";
    
    public static final String EVENTMEDIATYPE = "application/vnd.apache.jackrabbit.event+xml";

    private static Logger log = LoggerFactory.getLogger(EventJournalResourceImpl.class);

    private final HttpServletRequest request;
    private final EventJournal journal;
    private final DavResourceLocator locator;

    EventJournalResourceImpl(EventJournal journal, DavResourceLocator locator, JcrDavSession session,
            HttpServletRequest request, DavResourceFactory factory) {
        super(locator, session, factory);
        this.journal = journal;
        this.locator = locator;
        this.request = request;
    }

    @Override
    public String getSupportedMethods() {
        return "GET, HEAD";
    }

    @Override
    public boolean exists() {
        try {
            List<String> available = Arrays.asList(getRepositorySession().getWorkspace().getAccessibleWorkspaceNames());
            return available.contains(getLocator().getWorkspaceName());
        } catch (RepositoryException e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return "event journal for " + getLocator().getWorkspaceName();
    }

    @Override
    public long getModificationTime() {
        return System.currentTimeMillis();
    }

    private static final String ATOMNS = AtomFeedConstants.NS_URI;
    private static final String EVNS = ObservationConstants.NAMESPACE.getURI();

    private static final String AUTHOR = AtomFeedConstants.XML_AUTHOR;
    private static final String CONTENT = AtomFeedConstants.XML_CONTENT;
    private static final String ENTRY = AtomFeedConstants.XML_ENTRY;
    private static final String FEED = AtomFeedConstants.XML_FEED;
    private static final String ID = AtomFeedConstants.XML_ID;
    private static final String LINK = AtomFeedConstants.XML_LINK;
    private static final String NAME = AtomFeedConstants.XML_NAME;
    private static final String TITLE = AtomFeedConstants.XML_TITLE;
    private static final String UPDATED = AtomFeedConstants.XML_UPDATED;

    private static final String E_EVENT = ObservationConstants.XML_EVENT;
    private static final String E_EVENTDATE = ObservationConstants.XML_EVENTDATE;
    private static final String E_EVENTIDENTIFIER = ObservationConstants.XML_EVENTIDENTIFIER;
    private static final String E_EVENTINFO = ObservationConstants.XML_EVENTINFO;
    private static final String E_EVENTTYPE = ObservationConstants.XML_EVENTTYPE;
    private static final String E_EVENTMIXINNODETYPE = ObservationConstants.XML_EVENTMIXINNODETYPE;
    private static final String E_EVENTPRIMARNODETYPE = ObservationConstants.XML_EVENTPRIMARNODETYPE;
    private static final String E_EVENTUSERDATA = ObservationConstants.XML_EVENTUSERDATA;

    private static final int MAXWAIT = 2000; // maximal wait time
    private static final int MAXEV = 10000; // maximal event number

    private static final Attributes NOATTRS = new AttributesImpl();

    @Override
    public void spool(OutputContext outputContext) throws IOException {

        Calendar cal = Calendar.getInstance(Locale.ENGLISH);

        try {
            outputContext.setContentType("application/atom+xml; charset=UTF-8");
            outputContext.setProperty("Vary", "If-None-Match");
            // TODO: Content-Encoding: gzip

            // find out where to start
            long prevts = -1;
            String inm = request.getHeader("If-None-Match");
            if (inm != null) {
                // TODO: proper parsing when comma-delimited
                inm = inm.trim();
                if (inm.startsWith("\"") && inm.endsWith("\"")) {
                    String tmp = inm.substring(1, inm.length() - 1);
                    try {
                        prevts = Long.parseLong(tmp, 16);
                        journal.skipTo(prevts);
                    } catch (NumberFormatException ex) {
                        // broken etag
                    }
                }
            }

            boolean hasPersistEvents = false;

            if (outputContext.hasStream()) {

                long lastts = -1;
                long now = System.currentTimeMillis();
                boolean done = false;

                // collect events
                List<Event> events = new ArrayList<Event>(MAXEV);

                while (!done && journal.hasNext()) {
                    Event e = journal.nextEvent();

                    hasPersistEvents |= e.getType() == Event.PERSIST;

                    if (e.getDate() != lastts) {
                        // consider stopping
                        if (events.size() > MAXEV) {
                            done = true;
                        }
                        if (e.getDate() > now + MAXWAIT) {
                            done = true;
                        }
                    }

                    if (!done && (prevts == -1 || e.getDate() >= prevts)) {
                        events.add(e);
                    }

                    lastts = e.getDate();
                }

                if (lastts >= 0) {
                    // construct ETag from newest event
                    outputContext.setETag("\"" + Long.toHexString(lastts) + "\"");
                }

                OutputStream os = outputContext.getOutputStream();
                StreamResult streamResult = new StreamResult(os);
                SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
                TransformerHandler th = tf.newTransformerHandler();
                Transformer s = th.getTransformer();
                s.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                s.setOutputProperty(OutputKeys.INDENT, "yes");
                s.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

                th.setResult(streamResult);

                th.startDocument();

                th.startElement(ATOMNS, FEED, FEED, NOATTRS);

                writeAtomElement(th, TITLE, "EventJournal for " + getLocator().getWorkspaceName());

                th.startElement(ATOMNS, AUTHOR, AUTHOR, NOATTRS);
                writeAtomElement(th, NAME, "Jackrabbit Event Journal Feed Generator");
                th.endElement(ATOMNS, AUTHOR, AUTHOR);

                String id = getFullUri(request);
                writeAtomElement(th, ID, id);

                AttributesImpl linkattrs = new AttributesImpl();
                linkattrs.addAttribute(null, "self", "self", "CDATA", id);
                writeAtomElement(th, LINK, linkattrs, null);

                cal.setTimeInMillis(lastts >= 0 ? lastts : now);
                String upd = ISO8601.format(cal);
                writeAtomElement(th, UPDATED, upd);

                String lastDateString = "";
                long lastTimeStamp = 0;
                long index = 0;

                AttributesImpl contentatt = new AttributesImpl();
                contentatt.addAttribute(null, "type", "type", "CDATA", EVENTMEDIATYPE);

                while (!events.isEmpty()) {

                    List<Event> bundle = null;
                    String path = null;
                    String op;

                    if (hasPersistEvents) {
                        bundle = new ArrayList<Event>();
                        Event e = null;
                        op = "operations";

                        do {
                            e = events.remove(0);
                            bundle.add(e);

                            // compute common path
                            if (path == null) {
                                path = e.getPath();
                            } else {
                                if (e.getPath() != null && e.getPath().length() < path.length()) {
                                    path = e.getPath();
                                }
                            }
                        } while (e.getType() != Event.PERSIST && !events.isEmpty());
                    } else {
                        // no persist events
                        Event e = events.remove(0);
                        bundle = Collections.singletonList(e);
                        path = e.getPath();
                        op = EventUtil.getEventName(e.getType());
                    }

                    Event firstEvent = bundle.get(0);

                    String entryupd = lastDateString;
                    if (lastTimeStamp != firstEvent.getDate()) {
                        cal.setTimeInMillis(firstEvent.getDate());
                        entryupd = ISO8601.format(cal);
                        index = 0;
                    } else {
                        index += 1;
                    }

                    th.startElement(ATOMNS, ENTRY, ENTRY, NOATTRS);

                    String entrytitle = op + (path != null ? (": " + path) : "");
                    writeAtomElement(th, TITLE, entrytitle);

                    String entryid = id + "?type=journal&ts=" + Long.toHexString(firstEvent.getDate()) + "-" + index;
                    writeAtomElement(th, ID, entryid);

                    String author = firstEvent.getUserID() == null || firstEvent.getUserID().length() == 0 ? null
                            : firstEvent.getUserID();
                    if (author != null) {
                        th.startElement(ATOMNS, AUTHOR, AUTHOR, NOATTRS);
                        writeAtomElement(th, NAME, author);
                        th.endElement(ATOMNS, AUTHOR, AUTHOR);
                    }

                    writeAtomElement(th, UPDATED, entryupd);

                    th.startElement(ATOMNS, CONTENT, CONTENT, contentatt);

                    for (Event e : bundle) {

                        // serialize the event
                        th.startElement(EVNS, E_EVENT, E_EVENT, NOATTRS);

                        // DAV:href
                        if (e.getPath() != null) {
                            boolean isCollection = (e.getType() == Event.NODE_ADDED || e.getType() == Event.NODE_REMOVED);
                            String href = locator
                                    .getFactory()
                                    .createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(),
                                            e.getPath(), false).getHref(isCollection);
                            th.startElement(DavConstants.NAMESPACE.getURI(), DavConstants.XML_HREF,
                                    DavConstants.XML_HREF, NOATTRS);
                            th.characters(href.toCharArray(), 0, href.length());
                            th.endElement(DavConstants.NAMESPACE.getURI(), DavConstants.XML_HREF, DavConstants.XML_HREF);
                        }

                        // event type
                        String evname = EventUtil.getEventName(e.getType());
                        th.startElement(EVNS, E_EVENTTYPE, E_EVENTTYPE, NOATTRS);
                        th.startElement(EVNS, evname, evname, NOATTRS);
                        th.endElement(EVNS, evname, evname);
                        th.endElement(EVNS, E_EVENTTYPE, E_EVENTTYPE);

                        // date
                        writeObsElement(th, E_EVENTDATE, Long.toString(e.getDate()));

                        // user data
                        if (e.getUserData() != null && e.getUserData().length() > 0) {
                            writeObsElement(th, E_EVENTUSERDATA, firstEvent.getUserData());
                        }

                        // user id: already sent as Atom author/name

                        // try to compute nodetype information
                        if (e instanceof AdditionalEventInfo) {
                            try {
                                Name pnt = ((AdditionalEventInfo) e).getPrimaryNodeTypeName();
                                if (pnt != null) {
                                    writeObsElement(th, E_EVENTPRIMARNODETYPE, pnt.toString());
                                }

                                Set<Name> mixins = ((AdditionalEventInfo) e).getMixinTypeNames();
                                if (mixins != null) {
                                    for (Name mixin : mixins) {
                                        writeObsElement(th, E_EVENTMIXINNODETYPE, mixin.toString());
                                    }
                                }

                            } catch (UnsupportedRepositoryOperationException ex) {
                                // optional
                            }
                        }

                        // identifier
                        if (e.getIdentifier() != null) {
                            writeObsElement(th, E_EVENTIDENTIFIER, e.getIdentifier());
                        }

                        // info
                        if (!e.getInfo().isEmpty()) {
                            th.startElement(EVNS, E_EVENTINFO, E_EVENTINFO, NOATTRS);
                            Map<?, ?> m = e.getInfo();
                            for (Map.Entry<?, ?> entry : m.entrySet()) {
                                String key = entry.getKey().toString();
                                Object value = entry.getValue();
                                String t = value != null ? value.toString() : null;
                                writeElement(th, null, key, NOATTRS, t);
                            }
                            th.endElement(EVNS, E_EVENTINFO, E_EVENTINFO);
                        }

                        th.endElement(EVNS, E_EVENT, E_EVENT);

                        lastTimeStamp = e.getDate();
                        lastDateString = entryupd;
                    }

                    th.endElement(ATOMNS, CONTENT, CONTENT);
                    th.endElement(ATOMNS, ENTRY, ENTRY);
                }

                th.endElement(ATOMNS, FEED, FEED);

                th.endDocument();

                os.flush();
            }
        } catch (Exception ex) {
            throw new IOException("error generating feed: " + ex.getMessage());
        }
    }

    @Override
    public DavResource getCollection() {
        return null;
    }

    @Override
    public void addMember(DavResource resource, InputContext inputContext) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    @Override
    public DavResourceIterator getMembers() {
        return DavResourceIteratorImpl.EMPTY;
    }

    @Override
    public void removeMember(DavResource member) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    @Override
    protected void initLockSupport() {
        // lock not allowed
    }

    @Override
    protected String getWorkspaceHref() {
        return getHref();
    }

    private void writeElement(TransformerHandler th, String ns, String name, Attributes attrs, String textContent)
            throws SAXException {
        th.startElement(ns, name, name, attrs);
        if (textContent != null) {
            th.characters(textContent.toCharArray(), 0, textContent.length());
        }
        th.endElement(ns, name, name);
    }

    private void writeAtomElement(TransformerHandler th, String name, Attributes attrs, String textContent)
            throws SAXException {
        writeElement(th, ATOMNS, name, attrs, textContent);
    }

    private void writeAtomElement(TransformerHandler th, String name, String textContent) throws SAXException {
        writeAtomElement(th, name, NOATTRS, textContent);
    }

    private void writeObsElement(TransformerHandler th, String name, String textContent) throws SAXException {
        writeElement(th, EVNS, name, NOATTRS, textContent);
    }

    private String getFullUri(HttpServletRequest req) {

        String scheme = req.getScheme();
        int port = req.getServerPort();
        boolean isDefaultPort = (scheme.equals("http") && port == 80) || (scheme.equals("http") && port == 443);
        String query = request.getQueryString() != null ? "?" + request.getQueryString() : "";

        return String.format("%s://%s%s%s%s%s", scheme, req.getServerName(), isDefaultPort ? ":" : "",
                isDefaultPort ? Integer.toString(port) : "", req.getRequestURI(), query);
    }
}
