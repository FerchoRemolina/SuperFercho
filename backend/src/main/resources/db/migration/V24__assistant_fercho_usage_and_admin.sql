-- SuperFercho — Límites de uso, pausas y supervisión administrativa de Fercho.
-- Esquema propio del módulo assistant (sin FKs entre módulos; user_id referencia
-- lógicamente a identity.customers.id).

create schema if not exists assistant;

-- Un registro por turno solicitado a Fercho (aceptado o rechazado).
-- La ventana móvil de 24 h se calcula sobre status = 'ACCEPTED'.
create table assistant.fercho_chat_turns (
    id uuid primary key,
    user_id uuid not null,
    status varchar(24) not null,
    -- ACCEPTED | REJECTED_PAUSED | REJECTED_SUSPENDED | REJECTED_LIMIT | REJECTED_UNAVAILABLE
    llm_calls integer not null default 0,
    prompt_tokens integer,
    completion_tokens integer,
    estimated_cost numeric(12, 6),
    model varchar(64),
    created_at timestamptz not null default now()
);

create index idx_fercho_turns_user_status_created
    on assistant.fercho_chat_turns (user_id, status, created_at);

create index idx_fercho_turns_created
    on assistant.fercho_chat_turns (created_at);

-- Restricciones activas e históricas (pausas automáticas y suspensiones admin).
-- Nunca se eliminan: levantar una restricción marca lifted_at/lifted_by.
create table assistant.fercho_restrictions (
    id uuid primary key,
    user_id uuid not null,
    type varchar(24) not null,
    -- AUTO_PAUSE | ADMIN_SUSPENSION
    reason varchar(40) not null,
    -- LIMIT_EXCEEDED | ADMIN_DECISION
    created_at timestamptz not null default now(),
    created_by uuid,
    expires_at timestamptz not null,
    lifted_at timestamptz,
    lifted_by uuid,
    note varchar(300)
);

create index idx_fercho_restrictions_user_created
    on assistant.fercho_restrictions (user_id, created_at desc);

create index idx_fercho_restrictions_active
    on assistant.fercho_restrictions (user_id)
    where lifted_at is null;

-- Alertas administrativas por reincidencia. Deduplicación garantizada en BD:
-- a lo sumo una alerta OPEN por cliente.
create table assistant.fercho_admin_alerts (
    id uuid primary key,
    user_id uuid not null,
    status varchar(16) not null,
    -- OPEN | DISMISSED | WARNED | SUSPENDED
    reason varchar(40) not null,
    -- RECIDIVE_LIMIT_EXCEEDED
    window_started_at timestamptz not null,
    accepted_requests integer not null,
    llm_calls integer not null default 0,
    prompt_tokens integer,
    completion_tokens integer,
    estimated_cost numeric(12, 6),
    model varchar(64),
    created_at timestamptz not null default now(),
    decided_at timestamptz,
    decided_by uuid,
    decision_note varchar(300),
    restriction_id uuid
);

create unique index uq_fercho_alerts_open_per_user
    on assistant.fercho_admin_alerts (user_id)
    where status = 'OPEN';

create index idx_fercho_alerts_status_created
    on assistant.fercho_admin_alerts (status, created_at desc);

-- Auditoría de decisiones administrativas.
create table assistant.fercho_admin_actions (
    id uuid primary key,
    alert_id uuid,
    target_user_id uuid not null,
    action varchar(20) not null,
    -- DISMISS | WARN | SUSPEND | LIFT_SUSPENSION
    admin_id uuid not null,
    note varchar(300),
    restriction_id uuid,
    created_at timestamptz not null default now()
);

create index idx_fercho_actions_target
    on assistant.fercho_admin_actions (target_user_id, created_at desc);

-- Retención: los turnos de más de 30 días pueden purgarse con un job futuro
-- (fuera de alcance); las restricciones, alertas y acciones se conservan como
-- historial administrativo.
