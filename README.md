# Yami

This is the source code of the backend server of the social media network Yami.

To run it, you must provide a .env file in the following format:

```shell
PGHOST=your.host
PGPORT=5432 # (or your custom port)
PGDATABASE=your_db_name
PGUSER=your_db_user
PGPASSWORD=your_db_password
PGSSLMODE=require
PRODUCTION=false # true or false

B2_APPLICATION_KEY_ID=your_backblaze_b2_app_key_id
B2_APPLICATION_KEY=your_backblaze_b2_app_key
B2_BUCKET_NAME=your_backblaze_b2_bucket_name
B2_BUCKET_KEY_NAME=your_backblaze_b2_bucket_key_name
```

If you have a local postgres db on port 5432 with:
- username as postgres
- password as postgres
- db name as postgres

you can omit the PG environ vars.
