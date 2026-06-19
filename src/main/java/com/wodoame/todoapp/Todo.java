package com.wodoame.todoapp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "todos")
@Getter
@Setter
public class Todo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean completed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
