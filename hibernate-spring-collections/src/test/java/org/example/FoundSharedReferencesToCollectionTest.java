package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import lombok.Data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"/test-config.xml", "/FoundSharedReferencesToCollectionTest-config.xml"})
class FoundSharedReferencesToCollectionTest {

  @Autowired
  private UserDao userDao;

  @Autowired
  private DepartmentDao departmentDao;

  @Autowired
  private DepartmentPhoneDao departmentPhoneDao;

  @Autowired
  private PlatformTransactionManager transactionManager;

  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void init() {
    transactionTemplate = new TransactionTemplate(transactionManager);

    Department department = new Department();
    departmentDao.saveOrUpdate(department);

    DepartmentPhone departmentPhone1 = new DepartmentPhone();
    departmentPhone1.setDepartmentId(department.getId());
    departmentPhoneDao.saveOrUpdate(departmentPhone1);

    DepartmentPhone departmentPhone2 = new DepartmentPhone();
    departmentPhone2.setDepartmentId(department.getId());
    departmentPhoneDao.saveOrUpdate(departmentPhone2);

    User user1 = new User();
    user1.setDepartmentId(department.getId());
    userDao.saveOrUpdate(user1);

    User user2 = new User();
    user2.setDepartmentId(department.getId());
    userDao.saveOrUpdate(user2);
  }

  @AfterEach
  void tearDown() {
    userDao.deleteAll();
  }

  @Test
  void sample() {
    Exception exception = assertThrows(JpaSystemException.class, () -> {
      transactionTemplate.executeWithoutResult(status -> {
        List<User> users = userDao.findAll();

        Department department = new Department();
        departmentDao.saveOrUpdate(department);
        User user = new User();
        user.setDepartmentId(department.getId());
        userDao.saveOrUpdate(user);
      });
    });

    assertThat(exception.getMessage(), startsWith("Found shared references to a collection"));
  }

  public static class UserDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveOrUpdate(User user) {
      if (user.getId() == 0) {
        entityManager.persist(user);
      } else {
        entityManager.merge(user);
      }
    }

    public List<User> findAll() {
      TypedQuery<User> query = entityManager.createQuery("from FoundSharedReferencesToCollectionTest$User u join fetch u.department join fetch u.departmentPhones", User.class);
      return query.getResultList();
    }

    @Transactional
    public void deleteAll() {
      entityManager.createQuery("delete from FoundSharedReferencesToCollectionTest$User").executeUpdate();
    }
  }

  public static class DepartmentDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveOrUpdate(Department department) {
      if (department.getId() == 0) {
        entityManager.persist(department);
      } else {
        entityManager.merge(department);
      }
    }
  }

  public static class DepartmentPhoneDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveOrUpdate(DepartmentPhone departmentPhone) {
      if (departmentPhone.getId() == 0) {
        entityManager.persist(departmentPhone);
      } else {
        entityManager.merge(departmentPhone);
      }
    }
  }

  @Data
  @Entity
  public static class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "department_id")
    private int departmentId;

    @ManyToOne
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Department department;

    @OneToMany
    @JoinColumn(name = "department_id", referencedColumnName = "department_id", insertable = false, updatable = false)
    private Set<DepartmentPhone> departmentPhones;
  }

  @Data
  @Entity
  public static class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
  }

  @Data
  @Entity
  public static class DepartmentPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "department_id")
    private int departmentId;
  }
}
