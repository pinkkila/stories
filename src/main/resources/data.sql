delete
from story;

insert into story (id, story_title, story_text)
values (99, 'Example', 'This is example story');


SELECT setval('story_id_seq', (SELECT MAX(id) FROM story));