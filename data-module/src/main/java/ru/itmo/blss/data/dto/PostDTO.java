package ru.itmo.blss.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostDTO {
    public String title;
    public String topic;
    public String payload;
}
