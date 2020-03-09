import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

// LRParser.java
// CSCI 530: Project 2
// By Konrad Wiley (rkwiley)
// A Simple LR Parser
// for the following grammar:
// E -> E+T
// E -> T
// T -> T*F
// F -> (E)
// F -> id

public class LRParser{
    private ArrayList<Map<String, TableVal>> actionTable;
    private ArrayList<Map<Character, Integer>> gotoTable;
    private Stack<Object> stack;
    private String inputString;
    private int initLength;

    public static void main(String[] args){
        Scanner kb = new Scanner(System.in);
        String input;
        System.out.println("LR Parser by Konrad Wiley");
        System.out.println("Enter your sentential form, followed by $");
        //check for line without $ to terminate
        while((input=kb.nextLine()).endsWith("$")){
            // prints headers with 2x input length column width for stack
            String formatting = "%-"+Integer.toString(input.length()*2)+"s%-"+Integer.toString(input.length()+4)+"s%-6s\n";
            System.out.printf(formatting,"Stack", "Input", "Action");
            LRParser parser = new LRParser(input);
            while(parser.hasNext()){
                System.out.println(parser.parse());
            }
        }
        kb.close();
    }
    // constructor for class
    // populates <actionTable> and <gotoTable>
    // initializes stack, receives input string
    public LRParser(String strIn){
        //create table entries
        actionTable = new ArrayList<Map<String, TableVal>>();
        gotoTable = new ArrayList<Map<Character, Integer>>();
        this.buildTable();
        //initialize stack with 0
        stack = new Stack<Object>();
        stack.push(0);
        //receive input and save length (for formatting)
        inputString = strIn;
        initLength = strIn.length();
        
    }
    // a tuple class to store state and action
    public class TableVal{
        public Integer stateNum;
        public Action act;
        
        public TableVal(char a, Integer num){
            if(a == 'S')
                act = Action.S;
            else if (a == 'R')
                act = Action.R;
            else
                act = Action.ACCEPT;
            stateNum = num;            
        }
        public String toString(){
            if(act == Action.ACCEPT) return act.name();
            return act.name() + Integer.toString(stateNum);
        }
    }
    // an enum for TableVal
    public enum Action{R,S,ACCEPT};
    // creates LR table for <actionTable> and <gotoTable>
    public void buildTable(){
        for(int i = 0; i < 12; ++i){
            Map<String,TableVal> actionRow = new HashMap<String, TableVal>();
            Map<Character, Integer> gotoRow = new HashMap<Character, Integer>();

            switch(i){
                case 0:                    
                    actionRow.put("id", new TableVal('S', 5));
                    actionRow.put("(", new TableVal('S',4));
                    gotoRow.put('E', 1);
                    gotoRow.put('T', 2);
                    gotoRow.put('F', 3);                    
                    break;
                case 1:
                    actionRow.put("+",new TableVal('S',6));
                    actionRow.put("$", new TableVal('A', 0)); // CATCH THIS
                    break;
                case 2:
                    actionRow.put("+", new TableVal('R', 2));
                    actionRow.put("*", new TableVal('S',7));
                    actionRow.put(")", new TableVal('R',2));
                    actionRow.put("$", new TableVal('R',2));
                    break;
                case 3:
                    TableVal r4 = new TableVal('R',4);
                    actionRow.put("+",r4);
                    actionRow.put("*",r4);
                    actionRow.put(")",r4);
                    actionRow.put("$",r4);
                    break;
                case 4:
                    actionRow.put("id", new TableVal('S',5));
                    actionRow.put("(", new TableVal('S',4));
                    gotoRow.put('E', 8);
                    gotoRow.put('T', 2);
                    gotoRow.put('F', 3);
                    break;
                case 5:
                    TableVal r6 = new TableVal('R',6);
                    actionRow.put("+",r6);
                    actionRow.put("*",r6);
                    actionRow.put(")",r6);
                    actionRow.put("$",r6);
                    break;
                case 6:
                    actionRow.put("id",new TableVal('S',5));
                    actionRow.put("(", new TableVal('S',4));
                    gotoRow.put('T', 9);
                    gotoRow.put('F', 3);
                    break;
                case 7:
                    actionRow.put("id", new TableVal('S',5));
                    actionRow.put("(", new TableVal('S',4));
                    gotoRow.put('F', 10);
                    break;
                case 8:
                    actionRow.put("+",new TableVal('S',6));
                    actionRow.put(")", new TableVal('S',11));
                    break;
                case 9:
                    TableVal r1 = new TableVal('R',1);
                    actionRow.put("+",r1);
                    actionRow.put("*", new TableVal('S',7));
                    actionRow.put(")",r1);
                    actionRow.put("$",r1);
                    break;
                case 10:
                    TableVal r3 = new TableVal('R',3);
                    actionRow.put("+",r3);
                    actionRow.put("*",r3);
                    actionRow.put(")",r3);
                    actionRow.put("$",r3);
                    break;
                case 11:
                    TableVal r5 = new TableVal('R',5);
                    actionRow.put("+",r5);
                    actionRow.put("*",r5);
                    actionRow.put(")",r5);
                    actionRow.put("$",r5);
                    break;
            }
            actionTable.add(actionRow);
            gotoTable.add(gotoRow);
        }
    }

    // returns String with LR iteration
    public String parse(){
        String output = "";
        for(Object obj: stack)
            output += obj.toString();
        output = String.format("%-"+initLength*2+"s", output);
        output += String.format("%-"+(initLength+4)+"s", inputString);
        TableVal next = lookupAct();
        if(next == null){
            output+= String.format("%-6s", "ERROR");
            output+= "\nInvalid Entry";
            inputString = "";
            return output;
        }
        output += String.format("%-6s", next.toString());

        if(next.act==Action.R)
            rule(next.stateNum);
        else if (next.act == Action.S)
            shift(next.stateNum);
        else // next.act == Action.ACCEPT, finished
            removeNextToken();
        return output;
    }

    // checks if <inputString> is empty
    public boolean hasNext(){
        if(inputString.length() > 0)
            return true;
        return false;
    }

    // finds tableVal for next token+state
    private TableVal lookupAct(){
        Integer state = this.findInt();
        String token = this.getNextToken();
        return actionTable.get(state).get(token);
    }
    // finds Integer for next goTo state
    private Integer lookupGoTo(){
        Integer state = this.findInt();
        Character gotoChar = (Character)stack.peek();
        return gotoTable.get(state).get(gotoChar);
    }
    // performs shift op and puts <state> on stack
    private void shift(Integer state){
        stack.push(this.getNextToken());
        stack.push(state);
        this.removeNextToken();
    }
    // executes rule associated with <ruleNum>
    // pushes new state to stack
    private void rule(Integer ruleNum){
        switch(ruleNum){
            case 1:
                r1();
                break;
            case 2:
                r2();
                break;
            case 3:
                r3();
                break;
            case 4:
                r4();
                break;
            case 5:
                r5();
                break;
            case 6:
                r6();
        }
        // perform goTo;
        stack.push(lookupGoTo());
    }
    // executes E -> E+T
    private void r1(){
        Object obj;
        do{
            obj = stack.pop();
        }
        while(obj.getClass() != Character.class || !((Character)obj).equals('E'));
        stack.push(obj);
    }
    // executes E -> T
    private void r2(){
        Object obj;
        do{
            obj = stack.pop();
        }
        while(obj.getClass() != Character.class || !((Character)obj).equals('T'));
        stack.push((Character)'E');
    }
    // executes T -> T*F
    private void r3(){
        Object obj;
        do{
            obj = stack.pop();
        }
        while(obj.getClass() != Character.class || !((Character)obj).equals('T'));
        stack.push(obj);
    }
    // executes T -> F
    private void r4(){
        Object obj;
        do{
            obj = stack.pop();
        }
        while(obj.getClass() != Character.class || !((Character)obj).equals('F'));
        stack.push((Character)'T');
    }
    // executes F -> (E)
    private void r5(){
        Object obj;
        do{
            obj = stack.pop();
        }
        while(obj.getClass() != String.class || !((String)obj).equals("("));
        stack.push((Character)'F');
    }
    // executes F -> id
    private void r6(){
        Object obj;
        do{
            obj = stack.pop();
        }
        while(obj.getClass() != String.class || !((String)obj).equals("id"));
        stack.push((Character)'F');
    }
    // returns next int in <stack>
    private Integer findInt(){
        Integer result;
        Object obj = stack.pop();
        if (obj.getClass() != Integer.class)
            result = findInt();
        else result = (Integer)obj;
        stack.push(obj);
        return result;
    }
    // returns next token from <inputString>
    private String getNextToken(){
        if(inputString.charAt(0) == 'i')
            return "id";
        else
            return inputString.substring(0,1);
    }
    // deletes next token from <inputString>
    private void removeNextToken(){
        if(inputString.charAt(0) == 'i')
            inputString = inputString.substring(2);
        else
            inputString = inputString.substring(1);
    }    
}
