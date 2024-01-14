package org.example;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import lombok.Data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exception 'org.hibernate.HibernateException: A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance' is happening when the two
 * conditions are met (see https://stackoverflow.com/a/8835704 from
 * https://stackoverflow.com/questions/5587482/hibernate-a-collection-with-cascade-all-delete-orphan-was-no-longer-referenc):
 * <ol>
 *   <li>The entity, e.g. `Parent`, is managed.</li>
 *   <li>We assign a new collection to a field with a non-empty cascade element, e.g. `children`</li>
 * </ol>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"/test-config.xml", "/CollectionNoLongerReferencedTest-config.xml"})
class CollectionNoLongerReferencedTest {

  @Autowired
  private ParentDao parentDao;

  @Autowired
  private PlatformTransactionManager transactionManager;

  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void init() {
    Parent parent = new Parent();
    parentDao.saveOrUpdate(parent);

    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @AfterEach
  void tearDown() {
    parentDao.deleteAll();
  }

  @Test
  void sample() {
    Exception exception = assertThrows(JpaSystemException.class, () -> {
      transactionTemplate.executeWithoutResult(status -> {
        Parent parent = parentDao.findFirst();
        // The parent.children points to an instance of PersistentSet, now by setting a simple new HashSet<>() to this field when the parent entity is managed
        // we loose the reference to the initial PersistentSet instance
        parent.setChildren(new HashSet<>());

        parentDao.saveOrUpdate(parent);
      });
    });

    assertThat(exception.getMessage(), startsWith("A collection with cascade=\"all-delete-orphan\" was no longer referenced by the owning entity instance"));
  }

  public static class ParentDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveOrUpdate(Parent parent) {
      if (parent.getId() == 0) {
        entityManager.persist(parent);
      } else {
        entityManager.merge(parent);
      }
    }

    public Parent findFirst() {
      TypedQuery<Parent> query = entityManager.createQuery("from CollectionNoLongerReferencedTest$Parent", Parent.class).setMaxResults(1);
      return query.getResultList().iterator().next();
    }

    @Transactional
    public void deleteAll() {
      entityManager.createQuery("delete from CollectionNoLongerReferencedTest$Parent").executeUpdate();
    }
  }

  @Data
  @Entity
  public static class Parent {

    @Id
    @GeneratedValue
    private int id;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
    @Cascade({CascadeType.ALL, CascadeType.DELETE_ORPHAN })
    private Set<Child> children;
  }

  @Data
  @Entity
  public static class Child {
    @Id
    @GeneratedValue
    private int id;

    @ManyToOne
    private Parent parent;
  }
}
