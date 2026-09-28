package com.example.skillmatrix.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Employee name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "availability_status")
    private String availabilityStatus = "Available";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_skills", joinColumns = @JoinColumn(name = "employee_id"))
    private List<EmployeeSkill> skills = new ArrayList<>();

    public Employee() {
    }

    public Employee(String name, String email) {
        this.name = name;
        this.email = email;
        this.availabilityStatus = "Available";
    }

    public Employee(Long id, String name, String email, String availabilityStatus) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.availabilityStatus = availabilityStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public List<EmployeeSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<EmployeeSkill> skills) {
        this.skills = skills;
    }

    public void addSkill(Skill skill, Integer proficiency) {
        this.skills.add(new EmployeeSkill(skill, proficiency));
    }

    @Embeddable
    public static class EmployeeSkill {

        @NotNull(message = "Skill is required")
        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "skill_id", nullable = false)
        private Skill skill;

        @NotNull(message = "Proficiency is required")
        @Min(value = 1, message = "Proficiency must be between 1 and 5")
        @Max(value = 5, message = "Proficiency must be between 1 and 5")
        @Column(name = "proficiency", nullable = false)
        private Integer proficiency;

        public EmployeeSkill() {
        }

        public EmployeeSkill(Skill skill, Integer proficiency) {
            this.skill = skill;
            this.proficiency = proficiency;
        }

        public Skill getSkill() {
            return skill;
        }

        public void setSkill(Skill skill) {
            this.skill = skill;
        }

        public Integer getProficiency() {
            return proficiency;
        }

        public void setProficiency(Integer proficiency) {
            this.proficiency = proficiency;
        }
    }
}
