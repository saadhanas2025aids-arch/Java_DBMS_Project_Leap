package com.example.skillmatrix.service;

import com.example.skillmatrix.model.Allocation;
import com.example.skillmatrix.model.Employee;
import com.example.skillmatrix.model.Project;
import com.example.skillmatrix.model.Skill;
import com.example.skillmatrix.repository.AllocationRepository;
import com.example.skillmatrix.repository.EmployeeRepository;
import com.example.skillmatrix.repository.ProjectRepository;
import com.example.skillmatrix.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillMatrixService {

    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;
    private final AllocationRepository allocationRepository;

    public SkillMatrixService(EmployeeRepository employeeRepository,
                              SkillRepository skillRepository,
                              ProjectRepository projectRepository,
                              AllocationRepository allocationRepository) {
        this.employeeRepository = employeeRepository;
        this.skillRepository = skillRepository;
        this.projectRepository = projectRepository;
        this.allocationRepository = allocationRepository;
    }

    // ==========================================
    // SKILL CRUD
    // ==========================================

    public Skill createSkill(Skill skill) {
        if (skill.getName() == null || skill.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name cannot be empty");
        }
        Optional<Skill> existing = skillRepository.findByNameIgnoreCase(skill.getName().trim());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Skill with name '" + skill.getName() + "' already exists");
        }
        skill.setName(skill.getName().trim());
        return skillRepository.save(skill);
    }

    @Transactional(readOnly = true)
    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Skill getSkillById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found with ID: " + id));
    }

    public Skill updateSkill(Long id, Skill updatedSkill) {
        Skill skill = getSkillById(id);
        if (updatedSkill.getName() == null || updatedSkill.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name cannot be empty");
        }
        String newName = updatedSkill.getName().trim();
        Optional<Skill> existing = skillRepository.findByNameIgnoreCase(newName);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Another skill with name '" + newName + "' already exists");
        }
        skill.setName(newName);
        return skillRepository.save(skill);
    }

    public void deleteSkill(Long id) {
        Skill skill = getSkillById(id);

        // Clean up any employee skill references to maintain database integrity
        List<Employee> employees = employeeRepository.findAll();
        for (Employee emp : employees) {
            boolean removed = emp.getSkills().removeIf(es -> es.getSkill().getId().equals(id));
            if (removed) {
                employeeRepository.save(emp);
            }
        }

        // Clean up project required skill references
        List<Project> projects = projectRepository.findAll();
        for (Project proj : projects) {
            boolean removed = proj.getRequiredSkills().removeIf(ps -> ps.getSkill().getId().equals(id));
            if (removed) {
                projectRepository.save(proj);
            }
        }

        skillRepository.delete(skill);
    }

    // ==========================================
    // EMPLOYEE CRUD
    // ==========================================

    public Employee createEmployee(Employee employee) {
        if (employee.getName() == null || employee.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name cannot be empty");
        }
        if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee email cannot be empty");
        }
        String email = employee.getEmail().trim();
        if (employeeRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Employee with email '" + email + "' already exists");
        }
        employee.setName(employee.getName().trim());
        employee.setEmail(email);

        // Resolve skills and proficiencies
        resolveEmployeeSkills(employee);

        employee.setAvailabilityStatus("Available");
        return employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + id));
    }

    public Employee updateEmployee(Long id, Employee updated) {
        Employee employee = getEmployeeById(id);
        if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
            employee.setName(updated.getName().trim());
        }
        if (updated.getEmail() != null && !updated.getEmail().trim().isEmpty()) {
            String email = updated.getEmail().trim();
            Optional<Employee> existing = employeeRepository.findByEmailIgnoreCase(email);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Another employee already has email: " + email);
            }
            employee.setEmail(email);
        }

        if (updated.getSkills() != null) {
            employee.getSkills().clear();
            for (Employee.EmployeeSkill es : updated.getSkills()) {
                if (es.getSkill() != null && es.getSkill().getId() != null) {
                    Skill skill = getSkillById(es.getSkill().getId());
                    int prof = (es.getProficiency() != null && es.getProficiency() >= 1 && es.getProficiency() <= 5)
                            ? es.getProficiency() : 1;
                    employee.addSkill(skill, prof);
                }
            }
        }

        updateEmployeeAvailabilityStatus(employee);
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        // Remove associated allocations
        List<Allocation> allocations = allocationRepository.findByEmployeeId(id);
        allocationRepository.deleteAll(allocations);
        employeeRepository.delete(employee);
    }

    private void resolveEmployeeSkills(Employee employee) {
        List<Employee.EmployeeSkill> resolved = new ArrayList<>();
        if (employee.getSkills() != null) {
            for (Employee.EmployeeSkill es : employee.getSkills()) {
                if (es.getSkill() != null) {
                    Skill skill = null;
                    if (es.getSkill().getId() != null) {
                        skill = getSkillById(es.getSkill().getId());
                    } else if (es.getSkill().getName() != null) {
                        skill = skillRepository.findByNameIgnoreCase(es.getSkill().getName().trim())
                                .orElseGet(() -> createSkill(new Skill(es.getSkill().getName().trim())));
                    }
                    if (skill != null) {
                        int prof = (es.getProficiency() != null && es.getProficiency() >= 1 && es.getProficiency() <= 5)
                                ? es.getProficiency() : 1;
                        resolved.add(new Employee.EmployeeSkill(skill, prof));
                    }
                }
            }
        }
        employee.setSkills(resolved);
    }

    // ==========================================
    // PROJECT CRUD
    // ==========================================

    public Project createProject(Project project) {
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty");
        }
        project.setName(project.getName().trim());
        resolveProjectSkills(project);
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with ID: " + id));
    }

    public Project updateProject(Long id, Project updated) {
        Project project = getProjectById(id);
        if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
            project.setName(updated.getName().trim());
        }

        if (updated.getRequiredSkills() != null) {
            project.getRequiredSkills().clear();
            for (Project.ProjectSkill ps : updated.getRequiredSkills()) {
                if (ps.getSkill() != null && ps.getSkill().getId() != null) {
                    Skill skill = getSkillById(ps.getSkill().getId());
                    int prof = (ps.getProficiency() != null && ps.getProficiency() >= 1 && ps.getProficiency() <= 5)
                            ? ps.getProficiency() : 1;
                    project.addRequiredSkill(skill, prof);
                }
            }
        }
        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        Project project = getProjectById(id);
        // Remove associated allocations
        List<Allocation> allocations = allocationRepository.findByProjectId(id);
        allocationRepository.deleteAll(allocations);
        for (Allocation a : allocations) {
            updateEmployeeAvailabilityStatus(a.getEmployee());
            employeeRepository.save(a.getEmployee());
        }
        projectRepository.delete(project);
    }

    private void resolveProjectSkills(Project project) {
        List<Project.ProjectSkill> resolved = new ArrayList<>();
        if (project.getRequiredSkills() != null) {
            for (Project.ProjectSkill ps : project.getRequiredSkills()) {
                if (ps.getSkill() != null) {
                    Skill skill = null;
                    if (ps.getSkill().getId() != null) {
                        skill = getSkillById(ps.getSkill().getId());
                    } else if (ps.getSkill().getName() != null) {
                        skill = skillRepository.findByNameIgnoreCase(ps.getSkill().getName().trim())
                                .orElseGet(() -> createSkill(new Skill(ps.getSkill().getName().trim())));
                    }
                    if (skill != null) {
                        int prof = (ps.getProficiency() != null && ps.getProficiency() >= 1 && ps.getProficiency() <= 5)
                                ? ps.getProficiency() : 1;
                        resolved.add(new Project.ProjectSkill(skill, prof));
                    }
                }
            }
        }
        project.setRequiredSkills(resolved);
    }

    // ==========================================
    // ALLOCATION BUSINESS LOGIC
    // ==========================================

    public Allocation createAllocation(Allocation allocation) {
        if (allocation.getEmployee() == null || allocation.getEmployee().getId() == null) {
            throw new IllegalArgumentException("Employee ID is required for allocation");
        }
        if (allocation.getProject() == null || allocation.getProject().getId() == null) {
            throw new IllegalArgumentException("Project ID is required for allocation");
        }
        if (allocation.getAllocationPercentage() == null || allocation.getAllocationPercentage() <= 0) {
            throw new IllegalArgumentException("Allocation percentage must be greater than 0");
        }

        Employee employee = getEmployeeById(allocation.getEmployee().getId());
        Project project = getProjectById(allocation.getProject().getId());

        int currentTotal = getTotalAllocationForEmployee(employee.getId(), null);
        int newPercentage = allocation.getAllocationPercentage();

        if (currentTotal + newPercentage > 100) {
            throw new IllegalArgumentException("Allocation failed: Total allocation for employee '" + employee.getName()
                    + "' cannot exceed 100%. Current allocation: " + currentTotal
                    + "%, requested: " + newPercentage
                    + "%, remaining capacity: " + (100 - currentTotal) + "%");
        }

        allocation.setEmployee(employee);
        allocation.setProject(project);
        allocation.setAllocationPercentage(newPercentage);

        if (allocation.getAllocationStatus() == null || allocation.getAllocationStatus().trim().isEmpty()) {
            allocation.setAllocationStatus(newPercentage == 100 ? "Fully Allocated" : "Partially Allocated");
        }

        Allocation saved = allocationRepository.save(allocation);

        updateEmployeeAvailabilityStatus(employee);
        employeeRepository.save(employee);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Allocation> getAllAllocations() {
        return allocationRepository.findAll();
    }

    public Allocation updateAllocation(Long id, Allocation updated) {
        Allocation existing = allocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with ID: " + id));

        if (updated.getAllocationPercentage() == null || updated.getAllocationPercentage() <= 0) {
            throw new IllegalArgumentException("Allocation percentage must be greater than 0");
        }

        Long employeeId = existing.getEmployee().getId();
        int otherAllocationsTotal = getTotalAllocationForEmployee(employeeId, id);
        int newPercentage = updated.getAllocationPercentage();

        if (otherAllocationsTotal + newPercentage > 100) {
            throw new IllegalArgumentException("Allocation update failed: Total allocation for employee '"
                    + existing.getEmployee().getName()
                    + "' cannot exceed 100%. Other allocations total: " + otherAllocationsTotal
                    + "%, requested: " + newPercentage
                    + "%, available: " + (100 - otherAllocationsTotal) + "%");
        }

        existing.setAllocationPercentage(newPercentage);
        if (updated.getAllocationStatus() != null && !updated.getAllocationStatus().trim().isEmpty()) {
            existing.setAllocationStatus(updated.getAllocationStatus().trim());
        } else {
            existing.setAllocationStatus(newPercentage == 100 ? "Fully Allocated" : "Partially Allocated");
        }

        Allocation saved = allocationRepository.save(existing);

        updateEmployeeAvailabilityStatus(existing.getEmployee());
        employeeRepository.save(existing.getEmployee());

        return saved;
    }

    public void deleteAllocation(Long id) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with ID: " + id));

        Employee employee = allocation.getEmployee();
        allocationRepository.delete(allocation);

        updateEmployeeAvailabilityStatus(employee);
        employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public int getTotalAllocationForEmployee(Long employeeId, Long excludeAllocationId) {
        List<Allocation> allocations = allocationRepository.findByEmployeeId(employeeId);
        return allocations.stream()
                .filter(a -> excludeAllocationId == null || !a.getId().equals(excludeAllocationId))
                .mapToInt(Allocation::getAllocationPercentage)
                .sum();
    }

    private void updateEmployeeAvailabilityStatus(Employee employee) {
        int total = getTotalAllocationForEmployee(employee.getId(), null);
        if (total == 0) {
            employee.setAvailabilityStatus("Available");
        } else if (total < 100) {
            employee.setAvailabilityStatus("Partially Allocated (" + (100 - total) + "% remaining)");
        } else {
            employee.setAvailabilityStatus("Fully Allocated");
        }
    }

    // ==========================================
    // EMPLOYEE AVAILABILITY
    // ==========================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Employee emp : employees) {
            int currentAllocation = getTotalAllocationForEmployee(emp.getId(), null);
            if (currentAllocation < 100) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", emp.getId());
                map.put("name", emp.getName());
                map.put("email", emp.getEmail());
                map.put("currentAllocationPercentage", currentAllocation);
                map.put("remainingCapacityPercentage", 100 - currentAllocation);
                map.put("availabilityStatus", emp.getAvailabilityStatus());
                map.put("skills", emp.getSkills());
                result.add(map);
            }
        }
        return result;
    }

    // ==========================================
    // EMPLOYEE RECOMMENDATION ALGORITHM
    // ==========================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecommendations(Long projectId) {
        Project project = getProjectById(projectId);
        List<Project.ProjectSkill> requiredSkills = project.getRequiredSkills();

        if (requiredSkills.isEmpty()) {
            return Collections.emptyList();
        }

        List<Employee> allEmployees = employeeRepository.findAll();
        List<Map<String, Object>> recommendations = new ArrayList<>();

        for (Employee emp : allEmployees) {
            int currentAllocation = getTotalAllocationForEmployee(emp.getId(), null);
            int remainingCapacity = 100 - currentAllocation;

            // Only consider available employees (allocation < 100%)
            if (remainingCapacity <= 0) {
                continue;
            }

            int matchedSkillsCount = 0;
            double proficiencyScore = 0.0;
            List<Map<String, Object>> skillMatchDetails = new ArrayList<>();

            // Build map of employee skills for fast lookup
            Map<Long, Integer> empSkillMap = emp.getSkills().stream()
                    .collect(Collectors.toMap(
                            es -> es.getSkill().getId(),
                            Employee.EmployeeSkill::getProficiency,
                            (p1, p2) -> Math.max(p1, p2)
                    ));

            for (Project.ProjectSkill reqSkill : requiredSkills) {
                Long skillId = reqSkill.getSkill().getId();
                int reqProf = reqSkill.getProficiency();

                if (empSkillMap.containsKey(skillId)) {
                    matchedSkillsCount++;
                    int empProf = empSkillMap.get(skillId);
                    boolean meetsOrExceeds = empProf >= reqProf;

                    // Scoring preference:
                    // 1. Matched skill points
                    // 2. Proficiency meets or exceeds required proficiency (+ bonus for exceeding)
                    if (meetsOrExceeds) {
                        proficiencyScore += 15.0 + ((empProf - reqProf) * 3.0);
                    } else {
                        // Partial score if below required proficiency
                        proficiencyScore += ((double) empProf / reqProf) * 8.0;
                    }

                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("skillId", skillId);
                    detail.put("skillName", reqSkill.getSkill().getName());
                    detail.put("requiredProficiency", reqProf);
                    detail.put("employeeProficiency", empProf);
                    detail.put("meetsOrExceeds", meetsOrExceeds);
                    skillMatchDetails.add(detail);
                }
            }

            // If employee has at least one matching skill
            if (matchedSkillsCount > 0) {
                // Recommendation ranking formula:
                // Weight 1: Number of satisfied skills (30 points per skill)
                // Weight 2: Proficiency match quality (up to 15-21 points per skill)
                // Weight 3: Remaining availability (up to 20 points for 100% capacity)
                double skillMatchWeight = matchedSkillsCount * 30.0;
                double availabilityWeight = remainingCapacity * 0.2;
                double totalMatchScore = skillMatchWeight + proficiencyScore + availabilityWeight;

                Map<String, Object> rec = new LinkedHashMap<>();
                rec.put("employeeId", emp.getId());
                rec.put("employeeName", emp.getName());
                rec.put("email", emp.getEmail());
                rec.put("currentAllocation", currentAllocation);
                rec.put("remainingAvailability", remainingCapacity);
                rec.put("matchedSkillsCount", matchedSkillsCount);
                rec.put("totalRequiredSkills", requiredSkills.size());
                rec.put("matchScore", Math.round(totalMatchScore * 10.0) / 10.0);
                rec.put("matchedSkills", skillMatchDetails);

                recommendations.add(rec);
            }
        }

        // Rank employees from highest match score to lowest
        recommendations.sort((a, b) -> Double.compare((Double) b.get("matchScore"), (Double) a.get("matchScore")));

        return recommendations;
    }

    // ==========================================
    // SKILL COVERAGE ANALYSIS
    // ==========================================

    @Transactional(readOnly = true)
    public Map<String, Object> getSkillCoverage(Long projectId) {
        Project project = getProjectById(projectId);
        List<Project.ProjectSkill> requiredSkills = project.getRequiredSkills();
        List<Employee> allEmployees = employeeRepository.findAll();

        List<Map<String, Object>> coverageDetails = new ArrayList<>();
        int coveredCount = 0;

        for (Project.ProjectSkill reqSkill : requiredSkills) {
            Long skillId = reqSkill.getSkill().getId();
            String skillName = reqSkill.getSkill().getName();
            int reqProf = reqSkill.getProficiency();

            List<Map<String, Object>> matchingAvailableEmployees = new ArrayList<>();
            boolean isSkillCovered = false;

            for (Employee emp : allEmployees) {
                int currentAllocation = getTotalAllocationForEmployee(emp.getId(), null);
                int remainingCapacity = 100 - currentAllocation;

                // Employee is available if remaining capacity > 0
                if (remainingCapacity > 0) {
                    for (Employee.EmployeeSkill es : emp.getSkills()) {
                        if (es.getSkill().getId().equals(skillId)) {
                            boolean qualified = es.getProficiency() >= reqProf;
                            if (qualified) {
                                isSkillCovered = true;
                            }

                            Map<String, Object> empInfo = new LinkedHashMap<>();
                            empInfo.put("employeeId", emp.getId());
                            empInfo.put("employeeName", emp.getName());
                            empInfo.put("proficiency", es.getProficiency());
                            empInfo.put("remainingCapacity", remainingCapacity);
                            empInfo.put("meetsRequiredProficiency", qualified);
                            matchingAvailableEmployees.add(empInfo);
                            break;
                        }
                    }
                }
            }

            if (isSkillCovered) {
                coveredCount++;
            }

            String gapStatus;
            if (isSkillCovered) {
                gapStatus = "Covered";
            } else if (matchingAvailableEmployees.isEmpty()) {
                gapStatus = "Critical Gap: No available employee has this skill";
            } else {
                gapStatus = "Proficiency Gap: Available employees do not meet required proficiency (" + reqProf + ")";
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("skillId", skillId);
            item.put("skillName", skillName);
            item.put("requiredProficiency", reqProf);
            item.put("isCovered", isSkillCovered);
            item.put("gapStatus", gapStatus);
            item.put("availableMatchingEmployees", matchingAvailableEmployees);

            coverageDetails.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("projectId", project.getId());
        response.put("projectName", project.getName());
        response.put("totalRequiredSkills", requiredSkills.size());
        response.put("coveredSkillsCount", coveredCount);
        response.put("gapCount", requiredSkills.size() - coveredCount);
        response.put("isFullyCovered", requiredSkills.size() > 0 && coveredCount == requiredSkills.size());
        response.put("coverageDetails", coverageDetails);

        return response;
    }
}
