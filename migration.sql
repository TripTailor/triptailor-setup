CREATE TABLE IF NOT EXISTS attribute_location (
    attribute_id integer NOT NULL,
    location_id integer NOT NULL,
    location_rating double precision NOT NULL
);

CREATE INDEX IF NOT EXISTS attribute_location_attribute_id_idx ON attribute_location (attribute_id);
CREATE INDEX IF NOT EXISTS attribute_location_location_id_idx ON attribute_location (location_id);
