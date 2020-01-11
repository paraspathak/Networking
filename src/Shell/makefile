CXXFLAGS += --std=c++17
NCURSES = -lncurses

all: shell

shell: hw1.o
	${CXX} ${CXXFLAGS} -o shell hw1.o $(NCURSES)

hw1.o: hw1.cpp
	${CXX} ${CXXFLAGS} -c hw1.cpp $(NCURSES)

clean:
	-rm hw1.o ./shell
