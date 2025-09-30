delete
from story;

insert into story (id, story_title, story_text)
values (99, 'Example', 'This is an example story.'),
       (100, 'Another Example', 'This is an another example story.');


SELECT setval('story_id_seq', (SELECT MAX(id) FROM story));