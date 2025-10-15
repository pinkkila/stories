package com.pinkkila.stories.story;

//import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;


@Service
//@RequiredArgsConstructor
public class StoryService {
    private final StoryRepository storyRepository;
    private final RestClient restClient;
    
    
    public StoryService(StoryRepository storyRepository, RestClient.Builder builder) {
        this.storyRepository = storyRepository;
        this.restClient = builder
                .baseUrl("https://jsonplaceholder.typicode.com")
                .build();
    }
    
    public List<Story> findAll() {
        return storyRepository.findAll();
    }
    
    public void create(Story story) {
        storyRepository.save(story);
    }
    
    public record Post(int userId, Long id, String title, String body){}
    
    public Story getFromApi() {
        Post postFromApi =  restClient.get()
                .uri("/posts/1")
                .retrieve()
                .body(Post.class);
        assert postFromApi != null;
        return postToStory(postFromApi);
    }
    
    public Story postToStory(Post post) {
        return new Story(post.id, post.title, post.body);
    }
    
}