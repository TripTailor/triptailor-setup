\c triptailor

CREATE TABLE IF NOT EXISTS "review" (
  "id" SERIAL NOT NULL,
  "hostel_id" integer NOT NULL,
  "text" text NOT NULL
);

CREATE TABLE IF NOT EXISTS "attribute_review" (
  "attribute_id" integer NOT NULL,
  "review_id" integer NOT NULL,
  "positions" text NOT NULL,
  PRIMARY KEY(attribute_id, review_id)
);
