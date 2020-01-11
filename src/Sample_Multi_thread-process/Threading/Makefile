CXXFLAGS += --std=c++17 -pthread

all: sort

debug: CXXFLAGS += -g 
debug: sort

sort: threads.o
	${CXX} ${CXXFLAGS} -o sort threads.o 

threads.o: threads.cpp
	${CXX} ${CXXFLAGS} -c threads.cpp 

clean:
	-rm -f hw2.o ./sort *.o

rebuild: clean all
