import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;
import org.example.EmployeeFilterRequest;
import org.example.Main;
import org.example.employee.*;
import org.example.employee.Employee;
import org.example.employee.QEmployee;
import org.example.query.QueryFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = Main.class)
@Transactional
class FilterTest {

  @PersistenceContext private EntityManager entityManager;

  private JPAQueryFactory queryFactory;

  @BeforeEach
  void setUp() {
    queryFactory = new JPAQueryFactory(entityManager);

    // Clear tables for a fresh state before each test
    entityManager.createQuery("DELETE FROM Employee").executeUpdate();
    entityManager.createQuery("DELETE FROM Project").executeUpdate();
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

    Project project1 = new Project();
    project1.setName("Project Alpha");
    project1.setClient(client1);
    entityManager.persist(project1);

    Employee employee1 = new Employee();
    employee1.setName("John Doe");
    employee1.setDepartment(department1);
    employee1.setProjects(Set.of(project1));
    entityManager.persist(employee1);

    Project project2 = new Project();
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
    EmployeeFilterRequest filterRequest = new EmployeeFilterRequest();
    filterRequest.setName("John Doe");

    JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
    BooleanBuilder where =
        QueryFilterService.createDynamicPredicate(filterRequest, QEmployee.employee, query);

    List<Employee> employees = query.where(where).fetch();

    // Assert that only John Doe is returned
    assertEquals(1, employees.size());
    assertEquals("John Doe", employees.get(0).getName());
  }

  @Test
  void testByProjectNameFilter() {
    EmployeeFilterRequest filterRequest = new EmployeeFilterRequest();
    filterRequest.setProjectName("Project Alpha");

    JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
    BooleanBuilder where =
        QueryFilterService.createDynamicPredicate(filterRequest, QEmployee.employee, query);

    List<Employee> employees = query.where(where).fetch();

    // Assert that only John Doe is returned
    assertEquals(1, employees.size());
    assertEquals("John Doe", employees.get(0).getName());
  }

  @Test
  void testNestedJoinFiltering() {
    EmployeeFilterRequest filterRequest = new EmployeeFilterRequest();
    filterRequest.setClientName("Acme Corp");

    JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
    BooleanBuilder where =
        QueryFilterService.createDynamicPredicate(filterRequest, QEmployee.employee, query);

    List<Employee> employees = query.where(where).fetch();

    // Assert that two employees are linked to Acme Corp via Project Alpha
    assertEquals(1, employees.size());
    assertTrue(employees.stream().anyMatch(emp -> emp.getName().equals("John Doe")));
  }

  @Test
  void testMultipleFiltersApplied() {
    EmployeeFilterRequest filterRequest = new EmployeeFilterRequest();
    filterRequest.setName("John Doe");
    filterRequest.setClientName("Acme Corp");

    JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
    BooleanBuilder where =
        QueryFilterService.createDynamicPredicate(filterRequest, QEmployee.employee, query);

    List<Employee> employees = query.where(where).fetch();

    assertEquals(1, employees.size());
    assertEquals("John Doe", employees.get(0).getName());
  }

  @Test
  void testNullValuesAreIgnored() {
    EmployeeFilterRequest filterRequest = new EmployeeFilterRequest();
    filterRequest.setName(null); // Null field should be ignored
    filterRequest.setClientName("Beta Inc");

    JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
    BooleanBuilder where =
        QueryFilterService.createDynamicPredicate(filterRequest, QEmployee.employee, query);

    List<Employee> employees = query.where(where).fetch();

    // Assert that both employees linked to Acme Corp are returned
    assertEquals(1, employees.size());
    assertTrue(employees.stream().anyMatch(emp -> emp.getName().equals("Jane Smith")));
  }

  @Test
  void testDepartmentFilterApplied() {
    EmployeeFilterRequest filterRequest = new EmployeeFilterRequest();
    filterRequest.setDepartmentName("Marketing");

    JPAQuery<Employee> query = queryFactory.selectFrom(QEmployee.employee);
    BooleanBuilder where =
        QueryFilterService.createDynamicPredicate(filterRequest, QEmployee.employee, query);

    List<Employee> employees = query.where(where).fetch();

    // Assert that both employees linked to Acme Corp are returned
    assertEquals(1, employees.size());
    assertTrue(employees.stream().anyMatch(emp -> emp.getName().equals("Jane Smith")));
  }
}
