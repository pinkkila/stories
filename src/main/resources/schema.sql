drop table if exists story;

create table if not exists story
(
    id         bigserial primary key,
    story_title text not null,
    story_text text not null
);