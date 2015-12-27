\c triptailor_restaurants

ALTER TABLE "attribute"
  ALTER COLUMN name TYPE varchar(200);

CREATE TABLE IF NOT EXISTS "review" (
  "id" SERIAL NOT NULL,
  "hostel_id" integer NOT NULL,
  "text" text NOT NULL,
  "year" date,
  "reviewer" varchar(200),
  "city" varchar(200),
  "gender" varchar(100),
  "age" integer,
  PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS "attribute_review" (
  "attribute_id" integer NOT NULL,
  "review_id" integer NOT NULL,
  "positions" text NOT NULL,
  PRIMARY KEY(attribute_id, review_id)
);
