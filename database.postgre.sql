
-- you can migrate from mysql to postgresql by doing this:
-- podman run --rm -it -v /run/postgresql:/run/postgresql ghcr.io/dimitri/pgloader:latest pgloader mysql://rcrdit:rcrdit@10.16.19.1:3306/rcrdit postgresql://rcrdit:rcrdit@unix:/run/postgresql:5432/rcrdit


CREATE TABLE "profile" (
    "profile_id" SERIAL PRIMARY KEY,
    "name" character varying(64) NOT NULL,
    "folder" character varying(64) NOT NULL,
    "run_at_recording_start" character varying(64),
    "run_at_recording_finish" character varying(64)
);

CREATE TABLE "autorecs" (
    "autorec_id" SERIAL PRIMARY KEY,
    "profile_id" int NOT NULL,
    "priority" int DEFAULT '5' NOT NULL,
    "title" character varying(256),
    "channel_name" character varying(64),
    "days_of_week" character varying(64),
    "between_time_start" time without time zone,
    "between_time_end" time without time zone,
    "time_min" timestamptz,
    "time_max" timestamptz
);
