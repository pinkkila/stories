delete
from story;

insert into story (id, story_title, story_text)
values (99, 'Example', 'This is an example story.'),
       (100, 'Longer Example', 'This is a longer example story with more words in it. How does it flow. Does it go to the another line?');


SELECT setval('story_id_seq', (SELECT MAX(id) FROM story));