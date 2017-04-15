package org.kodejava.example.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.ReflectionDBObject;

import java.io.File;
import java.util.Objects;

/**
 * Created by elijahstaple on 4/14/17.
 */
public abstract class Profile extends ReflectionDBObject implements hasId {
    private int type;
    private String name;
    private int age;
    private String description;
    private dbImage profileImage;

    Profile(int type, String name, int age, String description, String profileImagePathname) {
        this.type = type;
        this.name = name;
        this.age = age;
        this.description = description;
        this.profileImage = new dbImage(profileImagePathname);
        if(Objects.equals(profileImage.getImageByteString(), "")) {
            System.out.println("Error encoding image file. Retry upload.");
        }
    }

    Profile(DBObject o) {
        BasicDBObject b = (BasicDBObject) o;
        this.type = b.getInt("Type");
        this.name = b.getString("Name");
        this.age = b.getInt("Age");
        this.description = b.getString("Description");
        this.profileImage = new dbImage((DBObject) o.get("ProfileImage"));
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public dbImage getProfileImage() {
        return profileImage;
    }

    public int setProfileImage(String profileImagePathname) {
        int errorCode;
        if((errorCode = profileImage.replaceImage(profileImagePathname)) != 0) {
            //TODO: Throw an error because it failed to replace the image
            if(errorCode == -1) {
                System.out.println("Failed to encode and couldn't delete newly created file");
            } else {
                System.out.println("Failed to encode but successfully delete newly created file");
            }
        }
        return errorCode;
    }

    public File getProfileImageFile(String pathname) {
        File file = new File(pathname);
        if(profileImage.getImageFile(file) != 0) {
            System.out.println("Error decoding image file.");
        }
        return file;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
