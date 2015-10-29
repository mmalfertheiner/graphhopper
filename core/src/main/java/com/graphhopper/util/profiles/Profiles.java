package com.graphhopper.util.profiles;

import com.graphhopper.util.profiles.operations.*;

public class Profiles {

    public static void main(String[] args) {

        String operationType = args[0];
        Operation op = null;

        System.out.println("Operation: " + args[0]);

        if(operationType.equalsIgnoreCase("create")){
            op = new CreateProfileOperation(args);
        } else if (operationType.equalsIgnoreCase("add")){
            op = new AddToProfileOperation(args);
        } else if (operationType.equalsIgnoreCase("print")){
            op = new PrintProfileOperation(args);
        }

        if(op != null)
            op.run();

    }
}
