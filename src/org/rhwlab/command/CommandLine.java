/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.command;
import java.lang.reflect.*;
import java.io.*;
/**
 *
 * @author gevirtzm
 */
/* Base class for implementing command line processing */
abstract public class CommandLine {
    static public boolean report = false;
    abstract public void init();   // will be executed before processing command line
    abstract public String post();   // will be executed after command line processing if no error occurred
    abstract public String noOption(String s); // method used for command line  parameter without an option
    abstract public void usage();  // printed summary of command line usage

    // sets the logging level
    public String L(String lev) {
        log.setLevel(lev);
        return null;
    }
    public String help(){
        usage();
        return null;
    }
    public void h() {
        help();
    }
    public String logfile(String fName){
        String ret = null;
        try {
            log.useStream(new PrintStream(new File(fName)));
        }
        catch (Exception ex){
            ret = String.format("Cannot open logfile: %s\n%s",fName,ex.getMessage());
        }
        return ret;
    }
    // process the command line arguments
    // returns null if no errors
    public String process(String [] args,boolean display) {
/*
        if (args.length == 0) {
            if (display) usage();
            return null;
        }
 
 */
        init();
        String ret = null;
        // get the list of methods that can be used to process the command line parameters
        Method[] methods = this.getClass().getMethods();

        // process each parameter
        for (int i=0 ; i<args.length ; ++i) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                // an option parameter - find a method that matches the option
                boolean found = false;
                for (Method method : methods) {
                    if (arg.substring(1).compareTo(method.getName())==0) {
                        found = true;
                        // a match - how many parameters does this method take?
                        int n = method.getParameterTypes().length;

                        // are there n more  parameters available?
                        if (i+n >= args.length) {
                            ret = String.format("Not enough arguments for option %s", arg);
                            if (display) {
                                System.out.println(ret);
                                usage();
                            }
                            return ret;
                        }
                        // check that the n parameters are not options
                        for (int j=0 ; j<n ; ++j) {
                            if (args[i+j+1].startsWith("-")) {
                                ret = String.format("Not enough arguments for option %s", arg);
                                if (display) {
                                    System.out.println(ret);
                                    usage();
                                }
                                return ret;
                            }
                        }

                        // yes - invoke the method with the n parameters
                        String[] params = new String[n];
                        for (int j=0 ; j<n ; ++j) {
                            params[j] = args[i+j+1];
                        }
                        try {
                            String res = (String)method.invoke(this, (java.lang.Object[])params);
                            if (res != null) {
                                if (display) {
                                    System.out.println(res);
                                    usage();
                                }
                                return res;
                            }
                            i = i + n;
                        }
                        catch (Exception e) {
                            ret = String.format("Exception invoking method: %s",method.getName());
                            if (display) {
                                System.out.println(ret);
                                usage();
                            }
                            return ret;
                        }
                    }
                }
                // no method matching the option was found
                if (!found) {
                    ret =  String.format("Unknown option: %s", arg);
                    if (display) {
                        System.out.println(ret);
                        usage();
                    }
                    return ret;
                }
            }
            // no option specified - use the noOption method
            else {
                noOption(arg);
            }
        }
        if (ret == null) {
            ret = post();
            if (display && ret !=null ) {
                System.out.println(ret);
                usage();
            }
        }
        return ret;
    }
    static public org.rhwlab.command.Logging log = new org.rhwlab.command.Logging();
}
