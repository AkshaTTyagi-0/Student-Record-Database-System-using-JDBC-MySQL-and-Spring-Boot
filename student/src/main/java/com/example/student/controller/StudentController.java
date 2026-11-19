
package com.example.student.controller;

import com.example.student.model.Student;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final Map<Long, Student> students =
            new ConcurrentHashMap<>();

    private final AtomicLong idGenerator =
            new AtomicLong(1);

    // 1. Add a student
    @PostMapping
    public ResponseEntity<Student> addStudent(
            @RequestBody Student student) {

        Long id = idGenerator.getAndIncrement();
        student.setId(id);

        students.put(id, student);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(student);
    }

    // 2. Retrieve all students
    @GetMapping
    public List<Student> getAllStudents() {
        return new ArrayList<>(students.values());
    }

    // 3. Search student by ID
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(
            @PathVariable Long id) {

        Student student = students.get(id);

        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(student);
    }

    // 4. Update student scores and skills
    @PutMapping("/{id}/performance")
    public ResponseEntity<Student> updateStudent(
            @PathVariable Long id,
            @RequestBody Student updatedStudent) {

        Student student = students.get(id);

        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        student.setScores(updatedStudent.getScores());
        student.setSkills(updatedStudent.getSkills());

        return ResponseEntity.ok(student);
    }

    // 5. Delete student record
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(
            @PathVariable Long id) {

        Student student = students.remove(id);

        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                "Student deleted successfully!");
    }

    // 6. Display merit list
    @GetMapping("/merit-list")
    public List<Student> getMeritList() {

        List<Student> meritList =
                new ArrayList<>(students.values());

        meritList.sort(
                Comparator.comparingDouble(
                        Student::getAverageScore
                ).reversed()
        );

        return meritList;
    }
}

