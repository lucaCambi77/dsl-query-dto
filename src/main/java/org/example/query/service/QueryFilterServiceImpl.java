package org.example.query.service;

import org.example.model.Client;
import org.example.model.Department;
import org.example.model.ProjectToDo;
import org.example.model.QEmployee;
import org.example.model.QProjectToDo;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.example.model.Employee.CLIENT;
import static org.example.model.Employee.DEPARTMENT;
import static org.example.model.Employee.PROJECTS;

@Service
public class QueryFilterServiceImpl implements QueryFilterService {

    Map<String, Class<?>> entities = Map.of(PROJECTS, ProjectToDo.class, CLIENT, Client.class, DEPARTMENT, Department.class);
    Map<String, String> expands = Map.of(PROJECTS, QEmployee.employee.projects.getMetadata().getName()
            , CLIENT, QProjectToDo.projectToDo.client.getMetadata().getName(), DEPARTMENT, QEmployee.employee.department.getMetadata().getName());

    @Override
    public String entityFrom(String expand) {
        return expands.get(expand);
    }

    @Override
    public Class<?> doFrom(String relation) {
        return entities.get(relation);
    }
}
