CXXFLAGS += --std=c++17


all: sort

sort: hw2.o
	${CXX} ${CXXFLAGS} -o sort hw2.o 

hw2.o: hw2.cpp
	${CXX} ${CXXFLAGS} -c hw2.cpp 

clean:
	-rm -f hw2.o ./sort *.cat ./0out ./1out ./2out ./3out ./4out ./5out ./out0 ./out1 ./out2 ./out3 ./out4 ./out5 ./output.txt ./a.out

rebuild: clean all
