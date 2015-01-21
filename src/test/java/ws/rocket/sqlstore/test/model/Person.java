/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.rocket.sqlstore.test.model;

import java.util.Date;

/**
 * This is a sample table model class used when testing.
 */
public final class Person {

  private Long id;

  private String name;

  private Date dateOfBirth;

  private boolean active;

  private Date dateCreated;

  private Long createdBy;

  private Date dateUpdated;

  private Long updatedBy;

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getDateOfBirth() {
    return this.dateOfBirth;
  }

  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public boolean isActive() {
    return this.active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Date getDateCreated() {
    return this.dateCreated;
  }

  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  public Long getCreatedBy() {
    return this.createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }

  public Date getDateUpdated() {
    return this.dateUpdated;
  }

  public void setDateUpdated(Date dateUpdated) {
    this.dateUpdated = dateUpdated;
  }

  public Long getUpdatedBy() {
    return this.updatedBy;
  }

  public void setUpdatedBy(Long updatedBy) {
    this.updatedBy = updatedBy;
  }

  @Override
  public int hashCode() {
    return this.id != null ? this.id.hashCode() : -1;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Person)) {
      return false;
    }

    Person o = (Person) obj;
    return equal(this.id, o.id)
        && equal(this.name, o.name)
        && equal(this.dateOfBirth, o.dateOfBirth)
        && this.active == o.active
        && equal(this.dateCreated, o.dateCreated)
        && equal(this.createdBy, o.createdBy)
        && equal(this.dateUpdated, o.dateUpdated)
        && equal(this.updatedBy, o.updatedBy);
  }

  private static boolean equal(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

}
