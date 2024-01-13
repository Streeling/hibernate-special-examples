package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;

import lombok.Data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"/test-config.xml"})
class MinimalTest {

  @PersistenceContext
  private EntityManager entityManager;

  @Test
  @Transactional
  void test() {
    SomeEntity someEntity = new SomeEntity();
    entityManager.persist(someEntity);

    List<SomeEntity> someEntityList = entityManager.createQuery("from MinimalTest$SomeEntity").getResultList();

    assertThat(someEntityList, hasSize(1));
  }

  @Data
  @Entity
  public static class SomeEntity {

    @Id
    private int id;
  }
}
