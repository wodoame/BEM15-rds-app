package com.wodoame.todoapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final TodoService todoService;

    @GetMapping("/")
    public String index(Model model) {
        List<Todo> todos = todoService.getAll();
        model.addAttribute("active", todos.stream().filter(t -> !t.isCompleted()).toList());
        model.addAttribute("completed", todos.stream().filter(Todo::isCompleted).toList());
        return "index";
    }

    @PostMapping("/todos")
    public String addTodo(@RequestParam String title,
                          @RequestParam(required = false) String description) {
        todoService.add(title, description);
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/toggle")
    public String toggleTodo(@PathVariable Long id) {
        todoService.toggleCompleted(id);
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/edit")
    public String editTodo(@PathVariable Long id,
                           @RequestParam String title,
                           @RequestParam(required = false) String description) {
        todoService.update(id, title, description);
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/delete")
    public String deleteTodo(@PathVariable Long id) {
        todoService.delete(id);
        return "redirect:/";
    }
}
