package com.example.skillmatrix.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Project name is required")
    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_skills", joinColumns = @JoinColumn(name = "project_id"))
    private List<ProjectSkill> requiredSkills = new ArrayList<>();

    public Project() {
    }

    public Project(String name) {
        this.name = name;
    }

    public Project(Long id, String name) {
        this.id = id;
        this.name = name;
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

    public List<ProjectSkill> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<ProjectSkill> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public void addRequiredSkill(Skill skill, Integer proficiency) {
        this.requiredSkills.add(new ProjectSkill(skill, proficiency));
    }

    @Embeddable
    public static class ProjectSkill {

        @NotNull(message = "Skill is required")
        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "skill_id", nullable = false)
        private Skill skill;

        @NotNull(message = "Required proficiency is required")
        @Min(value = 1, message = "Proficiency must be between 1 and 5")
        @Max(value = 5, message = "Proficiency must be between 1 and 5")
        @Column(name = "required_proficiency", nullable = false)
        private Integer proficiency;

        public ProjectSkill() {
        }

        public ProjectSkill(Skill skill, Integer proficiency) {
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
