package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

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

	public Number getPushedNumber(InstructionHandle instruction,ConstantPoolGen cpgen){
		//Currently gets value for ldc instructions only
		LDC loadInstruction = (LDC)instruction.getInstruction();
		Object value = loadInstruction.getValue(cpgen);
		return (Number)value;
	}

	enum binOps{
		IADD,FADD;
	}

	public boolean binaryOpFold(InstructionList il, ConstantPoolGen cpgen){
		boolean changed = false;
		System.out.println("Folding binary operation:");
		InstructionFinder itf = new InstructionFinder(il);
		//Search through InstructionList for pattern: load load followed by an arithmetic instruction
		Iterator iter = itf.search("ldc ldc ArithmeticInstruction");
		while (iter.hasNext()){
			//Iterator return InstructionHandle
			InstructionHandle[] instructions = (InstructionHandle[])iter.next();
			displayInfo("Old instruction segment:",2);
			for (InstructionHandle a : instructions){
				displayInfo(a.getInstruction().toString(),4);
			}
			Number[] operands = new Number[2];
			operands[0] = getPushedNumber(instructions[0],cpgen);
			operands[1] = getPushedNumber(instructions[1],cpgen);
			Instruction opcode = instructions[2].getInstruction();
			binOps opClass = binOps.valueOf(opcode.getClass().getSimpleName());
			Instruction newInstruction = null;
			switch (opClass){
				case IADD:
					newInstruction = new LDC(cpgen.addInteger(operands[0].intValue() + operands[1].intValue()));
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

	public Method optimizeMethod(ClassGen cgen, Method me){
		ConstantPoolGen cpgen = cgen.getConstantPool();
		MethodGen mg = new MethodGen(me, cgen.getClassName(), cpgen);
		System.out.println("\nDebugging method " + mg.getName());
		mg.removeNOPs();
		InstructionList il = mg.getInstructionList();
		System.out.println("Instructions before: ");
		System.out.println(il.toString());
		boolean changed = true;
		while (changed){
			changed = false;
			if (binaryOpFold(il,cpgen)){
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
		displayInfo("Integer Constant Pool before:",0);
		displayPool(cgen,cpgen);
		optimization(cgen);
		displayInfo("Integer Constant Pool after:",0);
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