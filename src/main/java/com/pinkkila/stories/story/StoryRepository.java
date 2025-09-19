package com.pinkkila.stories.story;

import org.springframework.data.repository.ListCrudRepository;

public interface StoryRepository extends ListCrudRepository<Story, Long> {
}
