package com.tareq.employee;

/**
 * Employee class for storing employees information
 */
public class Employee {
    public static final String MALE = "Male", FEMALE="Female";

    private int id;
    private String name;
    private int age;
    private int gender;

    public Employee(int id, String name, int age, int gender) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Returns gender in string correspond to int
     * @return  The gender string
     */
    public String getGenderStr() {
        if(gender==0)
            return MALE;
        else
            return FEMALE;

    }
    public int getGender(){
        return  gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }
}
