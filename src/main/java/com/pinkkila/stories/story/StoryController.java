package com.pinkkila.stories.story;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/story")
@RequiredArgsConstructor
public class StoryController {
    private final StoryService storyService;
    
    @PostMapping
    public String addStory(@ModelAttribute Story story, Model model) {
        storyService.create(new Story(null, story.getStoryTitle(), story.getStoryText()));
        model.addAttribute("story", story);
        return "redirect:/stories";
    }
    
}
