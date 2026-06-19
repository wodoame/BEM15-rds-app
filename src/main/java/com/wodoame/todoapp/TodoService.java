package com.wodoame.todoapp;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    @Cacheable("todos")
    public List<Todo> getAll() {
        return todoRepository.findAll();
    }

    @CacheEvict(value = "todos", allEntries = true)
    public void add(String title, String description) {
        Todo todo = new Todo();
        todo.setTitle(title.trim());
        todo.setDescription(normalize(description));
        todoRepository.save(todo);
    }

    @CacheEvict(value = "todos", allEntries = true)
    public void toggleCompleted(Long id) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
        todo.setCompleted(!todo.isCompleted());
        todoRepository.save(todo);
    }

    @CacheEvict(value = "todos", allEntries = true)
    public void update(Long id, String title, String description) {
        String trimmed = title.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
        todo.setTitle(trimmed);
        todo.setDescription(normalize(description));
        todoRepository.save(todo);
    }

    @CacheEvict(value = "todos", allEntries = true)
    public void delete(Long id) {
        todoRepository.deleteById(id);
    }

    private String normalize(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
