package com.innowise.specification;

import com.innowise.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {
    public static Specification<User> filterBy(String name, String surname, String email, Boolean active){
        return ((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(name != null && !name.isEmpty()){
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%"+name.toLowerCase()+"%"));
            }
            if(surname != null && !surname.isEmpty()){
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%"+surname.toLowerCase()+"%"));
            }
            if(email != null && !email.isEmpty()){
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%"+email.toLowerCase()+"%"));
            }
            if(active != null){
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
