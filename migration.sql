\c triptailor

CREATE TABLE IF NOT EXISTS "review" (
  "id" integer NOT NULL,
  "hostel_id" integer NOT NULL,
  "text" text NOT NULL
);

CREATE TABLE IF NOT EXISTS "attribute_review" (
  "attribute_id" integer NOT NULL,
  "review_id" integer NOT NULL
);

CREATE SEQUENCE review_id_seq;
SELECT setval('review_id_seq', max(id)) FROM review;
ALTER TABLE "review" ALTER COLUMN "id" SET DEFAULT nextval('review_id_seq');
