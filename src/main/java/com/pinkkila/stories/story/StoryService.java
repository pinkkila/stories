package com.pinkkila.stories.story;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryService {
    private final StoryRepository storyRepository;
    
    public List<Story> findAll() {
        return storyRepository.findAll();
    }
}
