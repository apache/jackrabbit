mysqladmin -f -uroot drop jackrabbit
mysqladmin -uroot create jackrabbit
mysql -uroot jackrabbit < create_db_mysql.sql