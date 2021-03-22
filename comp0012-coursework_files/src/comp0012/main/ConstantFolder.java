package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.*;
import org.apache.bcel.util.InstructionFinder;








public class ConstantFolder
{
	public static class VariableTable{
		private HashMap<Integer, Number> variableMap;
		public VariableTable(){
			variableMap = new HashMap<>();
		}

		public boolean hasPos(int storePos){
			return variableMap.containsKey(storePos);
		}

		public void setVar(int storePos, Number value){
			variableMap.put(storePos,value);
		}

		public Number getValue(int storePos){
			return variableMap.get(storePos);
		}


	}

	public static class Variables{
		public Variables(){

		}
	}


	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	public void printInstructions(ClassGen classGen,ConstantPoolGen constPoolGen){
		Method[] methods = classGen.getMethods();
		for (Method method : methods) {
			MethodGen methodGen = new MethodGen(method, classGen.getClassName(), constPoolGen);
			System.out.println(classGen.getClassName() + " > " + method.getName());
			System.out.println(methodGen.getInstructionList());
		}
	}

	public void getNumberConstant(ConstantPoolGen cpgen){
		ConstantPool cp = cpgen.getConstantPool();
		// get the constants in the pool
		Constant[] constants = cp.getConstantPool();
		for (int i = 0; i < constants.length; i++)
		{
			if (constants[i] instanceof ConstantInteger || constants[i] instanceof ConstantDouble || constants[i] instanceof ConstantLong || constants[i] instanceof ConstantFloat)
			{
				System.out.printf("%d) ",i);
				System.out.println(constants[i]);
			}
		}
		System.out.println();
	}

	public void displayPool(ClassGen cgen, ConstantPoolGen cpgen){

		//System.out.println(cpgen);
		getNumberConstant(cpgen);
		//printInstructions(cgen,cpgen);
	}

	public void displayInfo(String Content,int indent){
		System.out.println((new String(new char[indent]).replace("\0", " ")) + Content);
	}

	public Number getPushedNumber(InstructionHandle instructionHandler,ConstantPoolGen cpgen, VariableTable variableTable){
		//Gets the value that is being pushed from constant pool
		Object value = null;
		Instruction instruction = instructionHandler.getInstruction();
		if (instruction instanceof LDC){
			LDC loadInstruction = (LDC)instruction;
			value = loadInstruction.getValue(cpgen);
		}else if (instruction instanceof LDC2_W){
			LDC2_W loadInstruction = (LDC2_W)instruction;
			value = loadInstruction.getValue(cpgen);
		}else if (instruction instanceof LoadInstruction){
			LoadInstruction loadInstruction = (LoadInstruction) instruction;
			//Get value of local variable stored at the index referenced by Loadinstruction
			value = variableTable.getValue(loadInstruction.getIndex());
		}else if (instruction instanceof ConstantPushInstruction){
			ConstantPushInstruction loadInstruction = (ConstantPushInstruction) instruction;
			value = loadInstruction.getValue();
		}
		return (Number)value;
	}

	enum binOps{
		//Enum for binary operations, used to achieve switch case
		IADD,FADD,DADD,LADD,ISUB,FSUB,DSUB,LSUB,IDIV,FDIV,DDIV,LDIV,IMUL,FMUL,DMUL,LMUL;
	}

	public boolean binaryOpFold(InstructionList il, ConstantPoolGen cpgen,VariableTable variableTable){
		boolean changed = false;

		InstructionFinder itf = new InstructionFinder(il);
		//Search through InstructionList for pattern: load load followed by an arithmetic instruction
		//Iterator iter = itf.search("PushInstruction PushInstruction ArithmeticInstruction");
		Iterator iter = itf.search("PushInstruction PushInstruction ArithmeticInstruction");
		while (iter.hasNext()){
			//Iterator return InstructionHandle
			InstructionHandle[] instructions = (InstructionHandle[])iter.next();
			displayInfo("Old instruction segment:",2);
			for (InstructionHandle a : instructions){
				displayInfo(a.getInstruction().toString(),4);
			}
			Number[] operands = new Number[2];
			operands[0] = getPushedNumber(instructions[0],cpgen,variableTable);
			operands[1] = getPushedNumber(instructions[1],cpgen,variableTable);
			if (operands[0] == null || operands[1] == null){
				continue;
			}
			Instruction opcode = instructions[2].getInstruction();
			binOps opClass = binOps.valueOf(opcode.getClass().getSimpleName());
			Instruction newInstruction = null;
			switch (opClass){
				case IADD:
					newInstruction = new LDC(cpgen.addInteger(operands[0].intValue() + operands[1].intValue()));
					/*
					System.out.print(operands[0].intValue());
					System.out.print(" + ");
					System.out.print(operands[1].intValue());
					System.out.print(" = ");
					System.out.println(operands[0].intValue() + operands[1].intValue());*/
					break;
				case FADD:
					newInstruction = new LDC(cpgen.addFloat(operands[0].floatValue() + operands[1].floatValue()));
					break;
				case DADD:
					newInstruction = new LDC2_W(cpgen.addDouble(operands[0].doubleValue() + operands[1].doubleValue()));
					break;
				case LADD:
					newInstruction = new LDC2_W(cpgen.addLong(operands[0].longValue() + operands[1].longValue()));
					break;
				case ISUB:
					newInstruction = new LDC(cpgen.addInteger(operands[0].intValue() - operands[1].intValue()));
					break;
				case FSUB:
					newInstruction = new LDC(cpgen.addFloat(operands[0].floatValue() - operands[1].floatValue()));
					break;
				case DSUB:
					newInstruction = new LDC2_W(cpgen.addDouble(operands[0].doubleValue() - operands[1].doubleValue()));
					break;
				case LSUB:
					newInstruction = new LDC2_W(cpgen.addLong(operands[0].longValue() - operands[1].longValue()));
					break;
				case IDIV:
					newInstruction = new LDC(cpgen.addInteger(operands[0].intValue() / operands[1].intValue()));
					break;
				case FDIV:
					newInstruction = new LDC(cpgen.addFloat(operands[0].floatValue() / operands[1].floatValue()));
					break;
				case DDIV:
					newInstruction = new LDC2_W(cpgen.addDouble(operands[0].doubleValue() / operands[1].doubleValue()));
					break;
				case LDIV:
					newInstruction = new LDC2_W(cpgen.addLong(operands[0].longValue() / operands[1].longValue()));
					break;
				case IMUL:
					newInstruction = new LDC(cpgen.addInteger(operands[0].intValue() * operands[1].intValue()));
					break;
				case FMUL:
					newInstruction = new LDC(cpgen.addFloat(operands[0].floatValue() * operands[1].floatValue()));
					break;
				case DMUL:
					newInstruction = new LDC2_W(cpgen.addDouble(operands[0].doubleValue() * operands[1].doubleValue()));
					break;
				case LMUL:
					newInstruction = new LDC2_W(cpgen.addLong(operands[0].longValue() * operands[1].longValue()));
					break;
			}
			if (newInstruction != null){
				changed = true;
				displayInfo("New instruction segment:",2);
				displayInfo(newInstruction.toString(),4);
				instructions[0].setInstruction(newInstruction);
				try {
					il.delete(instructions[1]);
					il.delete(instructions[2]);
				}catch (TargetLostException e){
				}
			}else{
				displayInfo("Null newInstruction",2);
			}
		}
		System.out.println();
		return changed;
	}

	public ArrayList<Integer> localVarIndices(InstructionList il){
		InstructionFinder itf = new InstructionFinder(il);
		Iterator iter = itf.search("StoreInstruction");
		ArrayList<Integer> indices = new ArrayList<>();
		while (iter.hasNext()){
			InstructionHandle[] instructions = (InstructionHandle[])iter.next();
			StoreInstruction i = (StoreInstruction)instructions[0].getInstruction();
			indices.add(i.getIndex());
		}
		return indices;
	}


	public void localVar(InstructionList il, MethodGen mg, ConstantPoolGen cpgen){
		ArrayList<Integer> indices = localVarIndices(il);
		/*for (Integer a : indices){
			System.out.println(a);
		}*/

		LocalVariableGen[] lvg = mg.getLocalVariables();
		for (LocalVariableGen l : lvg){
			LocalVariable lv = l.getLocalVariable(cpgen);
			System.out.println(lv.toString());
		}
	}

	public void initVariableTable(InstructionList il,ConstantPoolGen cpgen,VariableTable variableTable){
		ArrayList<Integer> indices = localVarIndices(il);
		InstructionFinder itf = new InstructionFinder(il);
		Iterator iter = itf.search("PushInstruction StoreInstruction");
		while (iter.hasNext()) {
			InstructionHandle[] instructionHandler = (InstructionHandle[])iter.next();
			Number content = getPushedNumber(instructionHandler[0],cpgen,variableTable);
			StoreInstruction store = (StoreInstruction) instructionHandler[1].getInstruction();
			int storePos = store.getIndex();
			variableTable.setVar(storePos,content);
		}
	}

	public Method optimizeMethod(ClassGen cgen, Method me){
		ConstantPoolGen cpgen = cgen.getConstantPool();
		MethodGen mg = new MethodGen(me, cgen.getClassName(), cpgen);
		System.out.println("\nDebugging method " + mg.getName());
		mg.removeNOPs();
		InstructionList il = mg.getInstructionList();
		System.out.println("Instructions before: ");
		System.out.println(il.toString());
		VariableTable variableTable = new VariableTable();
		boolean changed = true;
		while (changed){
			//Fixed point
			initVariableTable(il,cpgen,variableTable);
			changed = false;
			System.out.println("Folding binary operation:");
			if (binaryOpFold(il,cpgen,variableTable)){
				changed = true;
			}
		}

		System.out.println("\nInstructions after");
		System.out.println(il.toString());
		return mg.getMethod();
	}

	public void optimization(ClassGen cgen){
		Method[] methods = cgen.getMethods();
		Method[] optimised = new Method[methods.length];
		for (int i = 0; i< methods.length; i++){
			optimised[i] = optimizeMethod(cgen,methods[i]);
		}
		gen.setMethods(optimised);
	}

	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		System.out.printf("*******%s*********\n",cgen.getClassName());
		ConstantPoolGen cpgen = cgen.getConstantPool();
		displayInfo("Number Constant Pool before:",0);
		displayPool(cgen,cpgen);
		optimization(cgen);
		displayInfo("Number Constant Pool after:",0);
		displayPool(cgen,cpgen);
		gen.setConstantPool(cpgen);
		this.optimized = gen.getJavaClass();
	}

	
	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}