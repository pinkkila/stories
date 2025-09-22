package com.pinkkila.stories.story;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Story {
    @Id
    private Long id;
    private String storyTitle;
    private String storyText;
}
