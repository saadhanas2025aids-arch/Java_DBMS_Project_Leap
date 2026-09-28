package com.example.skillmatrix;

import com.example.skillmatrix.model.Allocation;
import com.example.skillmatrix.model.Employee;
import com.example.skillmatrix.model.Project;
import com.example.skillmatrix.model.Skill;
import com.example.skillmatrix.repository.AllocationRepository;
import com.example.skillmatrix.repository.EmployeeRepository;
import com.example.skillmatrix.repository.ProjectRepository;
import com.example.skillmatrix.repository.SkillRepository;
import com.example.skillmatrix.service.SkillMatrixService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SkillMatrixApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillMatrixApplication.class, args);
    }

    /**
     * Seeds initial demo data for testing the application out of the box.
     * Uses existing repositories and service to avoid creating additional classes.
     */
    @Bean
    CommandLineRunner initDemoData(SkillRepository skillRepository,
                                  EmployeeRepository employeeRepository,
                                  ProjectRepository projectRepository,
                                  AllocationRepository allocationRepository,
                                  SkillMatrixService skillMatrixService) {
        return args -> {
            if (skillRepository.count() == 0) {
                // 1. Seed Skills
                Skill java = skillRepository.save(new Skill("Java"));
                Skill springBoot = skillRepository.save(new Skill("Spring Boot"));
                Skill react = skillRepository.save(new Skill("React"));
                Skill mySql = skillRepository.save(new Skill("MySQL"));
                Skill docker = skillRepository.save(new Skill("Docker"));
                Skill python = skillRepository.save(new Skill("Python"));

                // 2. Seed Employees with various skills and proficiencies (1=Beginner to 5=Expert)
                Employee alice = new Employee("Alice Johnson", "alice@example.com");
                alice.addSkill(java, 4);
                alice.addSkill(springBoot, 4);
                alice.addSkill(mySql, 3);
                alice = employeeRepository.save(alice);

                Employee bob = new Employee("Bob Smith", "bob@example.com");
                bob.addSkill(react, 5);
                bob.addSkill(java, 2);
                bob.addSkill(docker, 3);
                bob = employeeRepository.save(bob);

                Employee charlie = new Employee("Charlie Brown", "charlie@example.com");
                charlie.addSkill(python, 4);
                charlie.addSkill(mySql, 4);
                charlie.addSkill(docker, 4);
                charlie = employeeRepository.save(charlie);

                Employee diana = new Employee("Diana Prince", "diana@example.com");
                diana.addSkill(java, 5);
                diana.addSkill(springBoot, 5);
                diana.addSkill(docker, 4);
                diana.addSkill(mySql, 4);
                diana = employeeRepository.save(diana);

                // 3. Seed Projects with required skills and minimum required proficiencies
                Project project1 = new Project("Enterprise Cloud Portal");
                project1.addRequiredSkill(java, 4);
                project1.addRequiredSkill(springBoot, 3);
                project1.addRequiredSkill(docker, 3);
                project1 = projectRepository.save(project1);

                Project project2 = new Project("Data Analytics Engine");
                project2.addRequiredSkill(python, 4);
                project2.addRequiredSkill(mySql, 4);
                project2 = projectRepository.save(project2);

                // 4. Seed Allocations
                Allocation alloc1 = new Allocation(alice, project1, 50, "Partially Allocated");
                skillMatrixService.createAllocation(alloc1);

                Allocation alloc2 = new Allocation(bob, project1, 25, "Partially Allocated");
                skillMatrixService.createAllocation(alloc2);

                System.out.println(">>> Sample data initialized successfully for SkillMatrix application!");
            }
        };
    }
}
