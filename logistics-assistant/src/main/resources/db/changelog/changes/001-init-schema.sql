--liquibase formatted sql

--changeset routeplanner:001-postgis
--comment Spatial support for job.location. Needs the postgis/postgis image, not plain postgres.
CREATE EXTENSION IF NOT EXISTS postgis;

--changeset routeplanner:002-vehicle
CREATE TABLE vehicle (
    vehicle_id    BIGSERIAL PRIMARY KEY,
    license_plate VARCHAR(50)      NOT NULL UNIQUE,
    type          VARCHAR(50)      NOT NULL,
    capacity      DOUBLE PRECISION NOT NULL,
    status        VARCHAR(30)      NOT NULL DEFAULT 'AVAILABLE'
);

--changeset routeplanner:003-route
CREATE TABLE route (
    route_id       BIGSERIAL PRIMARY KEY,
    -- Nullable: createRoute and assignVehicle(routeId, vehicleId) are separate operations,
    -- so a route exists before a vehicle is attached to it.
    vehicle_id     BIGINT REFERENCES vehicle (vehicle_id),
    route_name     VARCHAR(100) NOT NULL,
    route_status   VARCHAR(30)  NOT NULL DEFAULT 'PLANNED',
    generated_time TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_route_vehicle_id ON route (vehicle_id);

--changeset routeplanner:004-job
CREATE TABLE job (
    job_id            BIGSERIAL PRIMARY KEY,
    -- Nullable, and load-bearing: jobs exist before any route does. POST /api/jobs (UC04) and
    -- the AI tool createDeliveryJob (FR02) both create unassigned jobs, and
    -- generateOptimisedRoute then consumes existing jobs to build the route.
    route_id          BIGINT REFERENCES route (route_id),
    job_name          VARCHAR(150) NOT NULL,
    delivery_location VARCHAR(255) NOT NULL,
    -- Not in the ERD: added because PostGIS needs coordinates and delivery_location is only an
    -- address string. Nullable — a job can exist with no coordinates yet (e.g. straight after
    -- AI-driven creation), with coordinates attached later via the Jobs module UI.
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    -- Single source of truth is latitude/longitude; this is derived, never written directly.
    location          GEOGRAPHY(Point, 4326)
                          GENERATED ALWAYS AS (
                              CASE
                                  WHEN latitude IS NOT NULL AND longitude IS NOT NULL
                                      THEN ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
                              END
                          ) STORED,
    sequence_order    INTEGER,
    priority          VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status            VARCHAR(30) NOT NULL DEFAULT 'PENDING'
);
CREATE INDEX idx_job_location ON job USING GIST (location);
CREATE INDEX idx_job_route_id ON job (route_id);

--changeset routeplanner:005-route-summary
CREATE TABLE route_summary (
    summary_id         BIGSERIAL PRIMARY KEY,
    route_id           BIGINT    NOT NULL UNIQUE REFERENCES route (route_id),
    total_distance     DOUBLE PRECISION,
    estimated_duration BIGINT,
    total_jobs         INTEGER,
    fuel_consumption   DOUBLE PRECISION,
    generated_time     TIMESTAMP NOT NULL DEFAULT now()
);

--changeset routeplanner:006-app-user
-- "user" is reserved in Postgres, hence app_user.
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'ROUTE_PLANNER',
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);
