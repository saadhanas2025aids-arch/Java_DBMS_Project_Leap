package com.example.skillmatrix.controller;

import com.example.skillmatrix.model.Allocation;
import com.example.skillmatrix.model.Employee;
import com.example.skillmatrix.model.Project;
import com.example.skillmatrix.model.Skill;
import com.example.skillmatrix.service.SkillMatrixService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SkillMatrixController {

    private final SkillMatrixService skillMatrixService;

    public SkillMatrixController(SkillMatrixService skillMatrixService) {
        this.skillMatrixService = skillMatrixService;
    }

    // ==========================================
    // 1. EMPLOYEE CRUD
    // ==========================================

    @PostMapping("/employees")
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody Employee employee) {
        Employee created = skillMatrixService.createEmployee(employee);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/employees")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(skillMatrixService.getAllEmployees());
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(skillMatrixService.getEmployeeById(id));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @Valid @RequestBody Employee employee) {
        return ResponseEntity.ok(skillMatrixService.updateEmployee(id, employee));
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Map<String, String>> deleteEmployee(@PathVariable Long id) {
        skillMatrixService.deleteEmployee(id);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", "Employee with ID " + id + " successfully deleted");
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. SKILL CRUD
    // ==========================================

    @PostMapping("/skills")
    public ResponseEntity<Skill> createSkill(@Valid @RequestBody Skill skill) {
        Skill created = skillMatrixService.createSkill(skill);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/skills")
    public ResponseEntity<List<Skill>> getAllSkills() {
        return ResponseEntity.ok(skillMatrixService.getAllSkills());
    }

    @GetMapping("/skills/{id}")
    public ResponseEntity<Skill> getSkillById(@PathVariable Long id) {
        return ResponseEntity.ok(skillMatrixService.getSkillById(id));
    }

    @PutMapping("/skills/{id}")
    public ResponseEntity<Skill> updateSkill(@PathVariable Long id, @Valid @RequestBody Skill skill) {
        return ResponseEntity.ok(skillMatrixService.updateSkill(id, skill));
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Map<String, String>> deleteSkill(@PathVariable Long id) {
        skillMatrixService.deleteSkill(id);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", "Skill with ID " + id + " successfully deleted");
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 3. PROJECT CRUD
    // ==========================================

    @PostMapping("/projects")
    public ResponseEntity<Project> createProject(@Valid @RequestBody Project project) {
        Project created = skillMatrixService.createProject(project);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(skillMatrixService.getAllProjects());
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(skillMatrixService.getProjectById(id));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id, @Valid @RequestBody Project project) {
        return ResponseEntity.ok(skillMatrixService.updateProject(id, project));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Map<String, String>> deleteProject(@PathVariable Long id) {
        skillMatrixService.deleteProject(id);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", "Project with ID " + id + " successfully deleted");
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 4. ALLOCATION CRUD
    // ==========================================

    @PostMapping("/allocations")
    public ResponseEntity<Allocation> createAllocation(@Valid @RequestBody Allocation allocation) {
        Allocation created = skillMatrixService.createAllocation(allocation);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/allocations")
    public ResponseEntity<List<Allocation>> getAllAllocations() {
        return ResponseEntity.ok(skillMatrixService.getAllAllocations());
    }

    @PutMapping("/allocations/{id}")
    public ResponseEntity<Allocation> updateAllocation(@PathVariable Long id, @Valid @RequestBody Allocation allocation) {
        return ResponseEntity.ok(skillMatrixService.updateAllocation(id, allocation));
    }

    @DeleteMapping("/allocations/{id}")
    public ResponseEntity<Map<String, String>> deleteAllocation(@PathVariable Long id) {
        skillMatrixService.deleteAllocation(id);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", "Allocation with ID " + id + " successfully deleted");
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 5. EMPLOYEE RECOMMENDATIONS
    // ==========================================

    @GetMapping("/projects/{projectId}/recommendations")
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(@PathVariable Long projectId) {
        return ResponseEntity.ok(skillMatrixService.getRecommendations(projectId));
    }

    // ==========================================
    // 6. SKILL COVERAGE
    // ==========================================

    @GetMapping("/projects/{projectId}/skill-coverage")
    public ResponseEntity<Map<String, Object>> getSkillCoverage(@PathVariable Long projectId) {
        return ResponseEntity.ok(skillMatrixService.getSkillCoverage(projectId));
    }

    // ==========================================
    // 7. EMPLOYEE AVAILABILITY
    // ==========================================

    @GetMapping("/employees/available")
    public ResponseEntity<List<Map<String, Object>>> getAvailableEmployees() {
        return ResponseEntity.ok(skillMatrixService.getAvailableEmployees());
    }

    // ==========================================
    // ERROR HANDLING
    // ==========================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        error.put("error", "Validation failed");
        error.put("details", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
