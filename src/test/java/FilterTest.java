
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.Main;
import org.example.model.Client;
import org.example.model.Department;
import org.example.model.Employee;
import org.example.model.ProjectToDo;
import org.example.model.QEmployee;
import org.example.query.service.QueryFilterService;
import org.example.query.QueryParams;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.example.model.Employee.DEPARTMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Main.class)
@Transactional
class FilterTest {

    @PersistenceContext
    private EntityManager entityManager;

    private JPAQueryFactory queryFactory;

    @Autowired
    private QueryFilterService queryFilterService;

    @BeforeEach
    void setUp() {
        queryFactory = new JPAQueryFactory(entityManager);

        // Clear tables for a fresh state before each test
        entityManager.createQuery("DELETE FROM Employee").executeUpdate();
        entityManager.createQuery("DELETE FROM ProjectToDo").executeUpdate();
        entityManager.createQuery("DELETE FROM EmployeeProject ").executeUpdate();
        entityManager.createQuery("DELETE FROM Client").executeUpdate();
        entityManager.createQuery("DELETE FROM Department").executeUpdate(); // Clear departments

        // Insert test data
        Department department1 = new Department();
        department1.setName("Engineering");
        entityManager.persist(department1);

        Department department2 = new Department();
        department2.setName("Marketing");
        entityManager.persist(department2);

        Client client1 = new Client();
        client1.setName("Acme Corp");
        entityManager.persist(client1);

        Client client2 = new Client();
        client2.setName("Beta Inc");
        entityManager.persist(client2);

        ProjectToDo project1 = new ProjectToDo();
        project1.setName("Project Alpha");
        project1.setClient(client1);
        entityManager.persist(project1);

        Employee employee1 = new Employee();
        employee1.setName("John Doe");
        employee1.setDepartment(department1);
        employee1.setProjects(Set.of(project1));
        entityManager.persist(employee1);

        ProjectToDo project2 = new ProjectToDo();
        project2.setName("Project Beta");
        project2.setClient(client2);
        entityManager.persist(project2);

        Employee employee2 = new Employee();
        employee2.setName("Jane Smith");
        employee2.setDepartment(department2); // Assigning department to employee
        employee2.setProjects(Set.of(project2));
        entityManager.persist(employee2);
    }

    @Test
    void testBasicEqualityFilter() {
        FilterRequestTest filterRequestTest = new FilterRequestTest();
        filterRequestTest.setName(List.of("John Doe"));

        JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
        Predicate where =
                queryFilterService.predicateFrom(QueryParams.from(filterRequestTest, null, null).queryFilter(), QEmployee.employee, query, List.of());

        List<Employee> employees = query.where(where).fetch();

        // Assert that only John Doe is returned
        assertEquals(1, employees.size());
        assertEquals("John Doe", employees.get(0).getName());
    }

    @Test
    void testByProjectNameFilter() {
        FilterRequestTest filterRequestTest = new FilterRequestTest();
        filterRequestTest.setProjectName(List.of("Project Alpha"));

        JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
        Predicate where =
                queryFilterService.predicateFrom(QueryParams.from(filterRequestTest, null, null).queryFilter(), QEmployee.employee, query, List.of());

        List<Employee> employees = query.where(where).fetch();

        // Assert that only John Doe is returned
        assertEquals(1, employees.size());
        assertEquals("John Doe", employees.get(0).getName());
    }

    @Test
    void testNestedJoinFiltering() {
        FilterRequestTest filterRequestTest = new FilterRequestTest();
        filterRequestTest.setClientName(List.of("Acme Corp"));

        JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
        Predicate where =
                queryFilterService.predicateFrom(QueryParams.from(filterRequestTest, null, null).queryFilter(), QEmployee.employee, query, List.of());

        List<Employee> employees = query.where(where).fetch();

        // Assert that two employees are linked to Acme Corp via Project Alpha
        assertEquals(1, employees.size());
        assertTrue(employees.stream().anyMatch(emp -> emp.getName().equals("John Doe")));
    }

    @Test
    void testMultipleFiltersApplied() {
        FilterRequestTest filterRequestTest = new FilterRequestTest();
        filterRequestTest.setName(List.of("John Doe"));
        filterRequestTest.setClientName(List.of("Acme Corp"));

        JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
        Predicate where =
                queryFilterService.predicateFrom(QueryParams.from(filterRequestTest, null, null).queryFilter(), QEmployee.employee, query, List.of());

        List<Employee> employees = query.where(where).fetch();

        assertEquals(1, employees.size());
        assertEquals("John Doe", employees.get(0).getName());
    }

    @Test
    void testNullValuesAreIgnored() {
        FilterRequestTest filterRequestTest = new FilterRequestTest();
        filterRequestTest.setName(null); // Null fieldName should be ignored
        filterRequestTest.setClientName(List.of("Beta Inc"));

        JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
        Predicate where =
                queryFilterService.predicateFrom(QueryParams.from(filterRequestTest, null, null).queryFilter(), QEmployee.employee, query, List.of());

        List<Employee> employees = query.where(where).fetch();

        // Assert that both employees linked to Acme Corp are returned
        assertEquals(1, employees.size());
        assertTrue(employees.stream().anyMatch(emp -> emp.getName().equals("Jane Smith")));
    }

    @Test
    void testDepartmentFilterApplied() {
        FilterRequestTest filterRequestTest = new FilterRequestTest();
        filterRequestTest.setDepartmentName(List.of("Marketing"));

        JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
        Predicate where =
                queryFilterService.predicateFrom(QueryParams.from(filterRequestTest, null, null).queryFilter(), QEmployee.employee, query, List.of());

        List<Employee> employees = query.where(where).fetch();

        // Assert that both employees linked to Acme Corp are returned
        assertEquals(1, employees.size());
        assertTrue(employees.stream().anyMatch(emp -> emp.getName().equals("Jane Smith")));
    }

    @Test
    void testExpandApplied() {
        JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
        Predicate where =
                queryFilterService.predicateFrom(QueryParams.from(null, null, null).queryFilter(), QEmployee.employee, query, List.of(DEPARTMENT));

        List<Employee> employees = query.where(where).fetch();

        assertFalse(Hibernate.isInitialized(employees.get(0).getProjects()));
        assertTrue(Hibernate.isInitialized(employees.get(0).getDepartment()));
    }
}
