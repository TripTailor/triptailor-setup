--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.2
-- Dumped by pg_dump version 9.5.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- Name: attribute_id_seq; Type: SEQUENCE; Schema: public; Owner: triptailor
--

CREATE SEQUENCE attribute_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attribute_id_seq OWNER TO triptailor;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: attribute; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE attribute (
    id integer DEFAULT nextval('attribute_id_seq'::regclass) NOT NULL,
    name character varying(1000) NOT NULL
);


ALTER TABLE attribute OWNER TO triptailor;

--
-- Name: attribute_location; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE attribute_location (
    attribute_id integer NOT NULL,
    location_id integer NOT NULL,
    location_rating double precision NOT NULL
);


ALTER TABLE attribute_location OWNER TO triptailor;

--
-- Name: attribute_review; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE attribute_review (
    attribute_id integer NOT NULL,
    review_id integer NOT NULL,
    positions text NOT NULL
);


ALTER TABLE attribute_review OWNER TO triptailor;

--
-- Name: attribute_search; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE attribute_search (
    attribute_id integer NOT NULL,
    search_id integer NOT NULL
);


ALTER TABLE attribute_search OWNER TO triptailor;

--
-- Name: hostel_id_seq; Type: SEQUENCE; Schema: public; Owner: triptailor
--

CREATE SEQUENCE hostel_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE hostel_id_seq OWNER TO triptailor;

--
-- Name: hostel; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE hostel (
    id integer DEFAULT nextval('hostel_id_seq'::regclass) NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    price double precision,
    images text DEFAULT NULL::character varying,
    url character varying(400) DEFAULT NULL::character varying,
    no_reviews integer NOT NULL,
    location_id integer NOT NULL,
    hostelworld_id integer,
    address character varying(300)
);


ALTER TABLE hostel OWNER TO triptailor;

--
-- Name: hostel_attribute; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE hostel_attribute (
    hostel_id integer NOT NULL,
    attribute_id integer NOT NULL,
    freq double precision NOT NULL,
    cfreq double precision NOT NULL,
    rating double precision NOT NULL
);


ALTER TABLE hostel_attribute OWNER TO triptailor;

--
-- Name: hostel_search; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE hostel_search (
    hostel_id integer NOT NULL,
    search_id integer NOT NULL,
    "timestamp" bigint NOT NULL
);


ALTER TABLE hostel_search OWNER TO triptailor;

--
-- Name: hostel_service; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE hostel_service (
    hostel_id integer NOT NULL,
    service_id integer NOT NULL
);


ALTER TABLE hostel_service OWNER TO triptailor;

--
-- Name: location_id_seq; Type: SEQUENCE; Schema: public; Owner: triptailor
--

CREATE SEQUENCE location_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE location_id_seq OWNER TO triptailor;

--
-- Name: location; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE location (
    id integer DEFAULT nextval('location_id_seq'::regclass) NOT NULL,
    city character varying(100) NOT NULL,
    country character varying(60) NOT NULL,
    state character varying(60) DEFAULT NULL::character varying,
    region character varying(60) DEFAULT NULL::character varying,
    continent character varying(60) DEFAULT NULL::character varying
);


ALTER TABLE location OWNER TO triptailor;

--
-- Name: play_evolutions_id_seq; Type: SEQUENCE; Schema: public; Owner: triptailor
--

CREATE SEQUENCE play_evolutions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE play_evolutions_id_seq OWNER TO triptailor;

--
-- Name: play_evolutions; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE play_evolutions (
    id integer DEFAULT nextval('play_evolutions_id_seq'::regclass) NOT NULL,
    hash character varying(510) NOT NULL,
    applied_at timestamp without time zone DEFAULT now() NOT NULL,
    apply_script text,
    revert_script text,
    state character varying(510) DEFAULT NULL::character varying,
    last_problem text
);


ALTER TABLE play_evolutions OWNER TO triptailor;

--
-- Name: review; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE review (
    id integer NOT NULL,
    hostel_id integer NOT NULL,
    text text NOT NULL,
    year date,
    reviewer character varying(200),
    city character varying(200),
    gender character varying(100),
    age integer,
    sentiment character varying(200),
    lat smallint,
    long smallint,
    sentiments jsonb,
    attributes jsonb
);


ALTER TABLE review OWNER TO triptailor;

--
-- Name: review_id_seq; Type: SEQUENCE; Schema: public; Owner: triptailor
--

CREATE SEQUENCE review_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE review_id_seq OWNER TO triptailor;

--
-- Name: review_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: triptailor
--

ALTER SEQUENCE review_id_seq OWNED BY review.id;


--
-- Name: search_id_seq; Type: SEQUENCE; Schema: public; Owner: triptailor
--

CREATE SEQUENCE search_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE search_id_seq OWNER TO triptailor;

--
-- Name: search; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE search (
    id integer DEFAULT nextval('search_id_seq'::regclass) NOT NULL,
    sess character varying(80) NOT NULL,
    city_id integer NOT NULL,
    hostel_id integer,
    "timestamp" bigint NOT NULL,
    adwords boolean NOT NULL
);


ALTER TABLE search OWNER TO triptailor;

--
-- Name: service_id_seq; Type: SEQUENCE; Schema: public; Owner: triptailor
--

CREATE SEQUENCE service_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE service_id_seq OWNER TO triptailor;

--
-- Name: service; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE service (
    id integer DEFAULT nextval('service_id_seq'::regclass) NOT NULL,
    name character varying(100) NOT NULL
);


ALTER TABLE service OWNER TO triptailor;

--
-- Name: share_id_seq; Type: SEQUENCE; Schema: public; Owner: triptailor
--

CREATE SEQUENCE share_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE share_id_seq OWNER TO triptailor;

--
-- Name: share; Type: TABLE; Schema: public; Owner: triptailor
--

CREATE TABLE share (
    id integer DEFAULT nextval('share_id_seq'::regclass) NOT NULL,
    session_id integer NOT NULL
);


ALTER TABLE share OWNER TO triptailor;

--
-- Name: id; Type: DEFAULT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY review ALTER COLUMN id SET DEFAULT nextval('review_id_seq'::regclass);


--
-- Name: attribute_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY attribute
    ADD CONSTRAINT attribute_pkey PRIMARY KEY (id);


--
-- Name: attribute_review_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY attribute_review
    ADD CONSTRAINT attribute_review_pkey PRIMARY KEY (attribute_id, review_id);


--
-- Name: attribute_search_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY attribute_search
    ADD CONSTRAINT attribute_search_pkey PRIMARY KEY (attribute_id, search_id);


--
-- Name: hostel_attribute_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel_attribute
    ADD CONSTRAINT hostel_attribute_pkey PRIMARY KEY (hostel_id, attribute_id);


--
-- Name: hostel_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel
    ADD CONSTRAINT hostel_pkey PRIMARY KEY (id);


--
-- Name: hostel_search_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel_search
    ADD CONSTRAINT hostel_search_pkey PRIMARY KEY (hostel_id, search_id, "timestamp");


--
-- Name: hostel_service_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel_service
    ADD CONSTRAINT hostel_service_pkey PRIMARY KEY (hostel_id, service_id);


--
-- Name: location_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY location
    ADD CONSTRAINT location_pkey PRIMARY KEY (id);


--
-- Name: play_evolutions_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY play_evolutions
    ADD CONSTRAINT play_evolutions_pkey PRIMARY KEY (id);


--
-- Name: review_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY review
    ADD CONSTRAINT review_pkey PRIMARY KEY (id);


--
-- Name: search_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY search
    ADD CONSTRAINT search_pkey PRIMARY KEY (id);


--
-- Name: service_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY service
    ADD CONSTRAINT service_pkey PRIMARY KEY (id);


--
-- Name: share_pkey; Type: CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY share
    ADD CONSTRAINT share_pkey PRIMARY KEY (id);


--
-- Name: attribute_review_attribute_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX attribute_review_attribute_id_idx ON attribute_review USING btree (attribute_id);


--
-- Name: attribute_review_review_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX attribute_review_review_id_idx ON attribute_review USING btree (review_id);


--
-- Name: attribute_search_attribute_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX attribute_search_attribute_id_idx ON attribute_search USING btree (attribute_id);


--
-- Name: attribute_search_search_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX attribute_search_search_id_idx ON attribute_search USING btree (search_id);


--
-- Name: hostel_attribute_attribute_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX hostel_attribute_attribute_id_idx ON hostel_attribute USING btree (attribute_id);


--
-- Name: hostel_attribute_hostel_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX hostel_attribute_hostel_id_idx ON hostel_attribute USING btree (hostel_id);


--
-- Name: hostel_location_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX hostel_location_id_idx ON hostel USING btree (location_id);


--
-- Name: hostel_search_hostel_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX hostel_search_hostel_id_idx ON hostel_search USING btree (hostel_id);


--
-- Name: hostel_search_search_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX hostel_search_search_id_idx ON hostel_search USING btree (search_id);


--
-- Name: hostel_service_hostel_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX hostel_service_hostel_id_idx ON hostel_service USING btree (hostel_id);


--
-- Name: hostel_service_service_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX hostel_service_service_id_idx ON hostel_service USING btree (service_id);


--
-- Name: review_hostel_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX review_hostel_id_idx ON review USING btree (hostel_id);


--
-- Name: search_city_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX search_city_id_idx ON search USING btree (city_id);


--
-- Name: search_hostel_id_idx; Type: INDEX; Schema: public; Owner: triptailor
--

CREATE INDEX search_hostel_id_idx ON search USING btree (hostel_id);


--
-- Name: attribute_search_ibfk_1; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY attribute_search
    ADD CONSTRAINT attribute_search_ibfk_1 FOREIGN KEY (attribute_id) REFERENCES attribute(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: attribute_search_ibfk_2; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY attribute_search
    ADD CONSTRAINT attribute_search_ibfk_2 FOREIGN KEY (search_id) REFERENCES search(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: hostel_attribute_ibfk_1; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel_attribute
    ADD CONSTRAINT hostel_attribute_ibfk_1 FOREIGN KEY (hostel_id) REFERENCES hostel(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: hostel_attribute_ibfk_2; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel_attribute
    ADD CONSTRAINT hostel_attribute_ibfk_2 FOREIGN KEY (attribute_id) REFERENCES attribute(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: hostel_ibfk_1; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel
    ADD CONSTRAINT hostel_ibfk_1 FOREIGN KEY (location_id) REFERENCES location(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: hostel_search_ibfk_1; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel_search
    ADD CONSTRAINT hostel_search_ibfk_1 FOREIGN KEY (hostel_id) REFERENCES hostel(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: hostel_search_ibfk_2; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel_search
    ADD CONSTRAINT hostel_search_ibfk_2 FOREIGN KEY (search_id) REFERENCES search(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: hostel_service_ibfk_1; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel_service
    ADD CONSTRAINT hostel_service_ibfk_1 FOREIGN KEY (hostel_id) REFERENCES hostel(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: hostel_service_ibfk_2; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY hostel_service
    ADD CONSTRAINT hostel_service_ibfk_2 FOREIGN KEY (service_id) REFERENCES service(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: search_ibfk_1; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY search
    ADD CONSTRAINT search_ibfk_1 FOREIGN KEY (city_id) REFERENCES location(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: search_ibfk_2; Type: FK CONSTRAINT; Schema: public; Owner: triptailor
--

ALTER TABLE ONLY search
    ADD CONSTRAINT search_ibfk_2 FOREIGN KEY (hostel_id) REFERENCES hostel(id) DEFERRABLE INITIALLY DEFERRED;


REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM triptailor;
GRANT ALL ON SCHEMA public TO triptailor;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

