package com.pinkkila.stories.page;

import com.pinkkila.stories.story.Story;
import com.pinkkila.stories.story.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {
    private final StoryService storyService;
    
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    @GetMapping("/write")
    public String write(Model model) {
        model.addAttribute("story", new Story());
        return "write";
    }
    
    @GetMapping("/stories")
    public String stories(Model model) {
        List<Story> stories = storyService.findAll();
        Story storyFromApi = storyService.getFromApi();
        stories.add(storyFromApi);
        storyService.getFromApi();
        model.addAttribute("stories", stories);
        return "stories";
    }
}
