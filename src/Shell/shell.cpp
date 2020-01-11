/*
    To run the code type g++ hw1.cpp -lncurses -g
    This code depends on the installation of ncurses library
    Author = @Paras Pathak
    Lab 1
*/



//C Library
#include <sys/types.h>
#include <sys/stat.h>   //Added for the stat command
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <dirent.h>
#include <string.h>
#include <time.h>
#include <curses.h>



//C++ Libraries
#include <vector>
#include <string>
#include <iostream>
#include <algorithm>

/*
    Author: Paras Pathak
    Class : CSE 3320-01 Summer 2019
    Simple shell with menu driven format
*/

/*
    Sources: Professor's sample code
    One function referenced from Stackoverflow, as noted below
*/


int x,y;


//operator overloading to handle comparing timespecs
bool operator <(const timespec& lhs, const timespec& rhs) {
    if (lhs.tv_sec == rhs.tv_sec)
        return lhs.tv_nsec < rhs.tv_nsec;
    else
        return lhs.tv_sec < rhs.tv_sec;
}

//Create and object of Folder to handle sort, size names and date
class Folder{
    public:
    Folder(std::string name, double size, timespec mod_time) : _name{name}, _size{size}, _modified_time{mod_time} {}
    std::string get_name(){
        return _name;
    }
    double get_size(){
        return _size;
    }
    timespec get_date(){
        return _modified_time;
    }
    private:
    std::string _name;
    double _size;
    timespec _modified_time;

};

std::vector<Folder*> folder_data;

//returns the size of the directory given its path
/*
    This function to give the size of the folder is
    referenced from stack overflow
    https://stackoverflow.com/questions/1129499/how-to-get-the-size-of-a-dir-programatically-in-linux
*/
double find_size_of_directory(std::string path){
    DIR *d;
    struct dirent *de;
    struct stat buf;
    int exists;
    int total_size;

    d = opendir(path.c_str());
    if (d == NULL) {
        perror("prsize");
        exit(1);
    }

    total_size = 0;

    for (de = readdir(d); de != NULL; de = readdir(d)) {
        exists = stat(de->d_name, &buf);
        if (exists < 0) {
        //fprintf(stderr, "Couldn't stat %s\n", de->d_name);
        } else {
        total_size += buf.st_size;
        }
    }
    closedir(d);
    //printf("%s %d\n", path.c_str(), total_size);
    return total_size;
}

//to compare two sizes function for std::sort
bool sort_size(Folder* first, Folder* second){
    return (first->get_size() > second->get_size());
}
//To compare two dates function for std::sort
bool sort_date(Folder* first, Folder* second){
    return (first->get_date() < second->get_date());
}

//To read user input from ncurses console
std::string read_string(){
    std::string input;
    // let the terminal do the line editing
    nocbreak();
    echo();
    char c;
    while ( c = getch() ){
        if(c == '\n') break;
        input.push_back( c );
    }
    return input;
}

//For next previous functionalities In work
void print_items(std::vector<std::string> f, int start){
    move(x,y);
    int a = y;
    //Clear the screen 8 lines
    for(int i =0; i<8; i++){
        clrtoeol();
        a++;
        move(x,a);
    }
    int counter =0;
    for (int i = start; f[i]!=f.back(); i++){
        printw("(");
        printw((std::to_string(i)).c_str());
        printw(")");
        printw(f[i].c_str());
        printw("\n");
        if(counter ==9) break;
        counter++;
    }
    for (; counter<9; counter++){
        printw("\n");
    }
}

int main(int argc, char** argv) {
    initscr();
	int size_of_buffer = 256;
    pid_t child;
    DIR * d;
	char abc;
    struct dirent * de;
    int i, c, k;
    char s[size_of_buffer], cmd[size_of_buffer];
    time_t t;
	bool get_new_data = true;
    std::vector<std::string> file;
    //std::vector<std::string> folder;
    int height, width;
	if(argc>1){	//case when argument is passed while running program
		std::string change_directory = "";
		for(int i=1; i<argc; i++){	//concatinate if there are spaces present between the filename
			if(i>1)
				change_directory +=" ";
			change_directory +=argv[i];
		}
		if(chdir(change_directory.c_str())==-1){
			if(errno == EACCES)
				std::cout<<"No accesss"<<std::endl;
			else if(errno == ENOTDIR)
				std::cout<<"Not a directory"<<std::endl;
			std::cout<<errno<<"cannot open directory as specified"<<change_directory<<std::endl;
		}
	}
	char user;
	std::string command;
    while (1) {
        clear();
        t = time( NULL );
        printw("Time: ");   printw(ctime(&t));

        //Get and print the working directory
        getcwd(s, (size_of_buffer-5));

        //Check if out of range
        if( errno == ERANGE){
            //"Larger buffer needed"
            size_of_buffer = size_of_buffer * 5;
            char new_buffer[size_of_buffer];
            getcwd(new_buffer,(size_of_buffer-5));
            printw("Current Direcotory:"); printw(new_buffer);
        }
        else {
            printw("Current Directory:"); printw(s);
        }

        if(get_new_data){
            //Open the directory
            d = opendir( "." );
            if(!d){
                printw("Cannot read this directory");
            }
            else{
                //Read directory and store it in corresponding vectors
                while (de = readdir(d)){
                    if (((de->d_type) & DT_REG)){
                        file.push_back(de->d_name);
                    }
                    else if(((de->d_type)& DT_DIR)){
                        std::string filename(de->d_name);
                        std::string pathname (s);
                        pathname = pathname + "/" + filename;
                        struct stat result;
                        stat(pathname.c_str(), &result);{
                            auto mod_time = result.st_mtim;
                            folder_data.push_back( new Folder(filename, find_size_of_directory(("./" + filename)),mod_time));
                        }
                        //folder.push_back(de->d_name);
                    }
                    else {
                        std::cout<<de->d_name<<"Type is other type";
                    }
                }
            }
            closedir( d );
		}
        c =0;
        printw("\nDirectory: \n");
        for (auto entry : folder_data){
            c++;
            printw("(");
            printw((std::to_string(c)).c_str());
            printw(")");
            printw(entry->get_name().c_str());
            printw("  ");
        }
        if(file.size()>8){
            getyx(stdscr,y,x);  //save the position before printing directories
        }
        c=0;
        int start_next_from = 0;
        printw("\nFiles: \n");

        for (auto entry : file){
            int height, width;
            //<!TODO>Handle the case of next page as well
            c++;
            printw("(");
            printw((std::to_string(c)).c_str());
            printw(")");
            printw(entry.c_str());
            printw("  ");
            getyx(stdscr,height,width);
            /*
            if(c==8) {
                start_next_from = 8;
                break;
            }
            */
        }
        //Operations
        printw("\n\nOperation: \n");
        printw("C Change Directory       D Display\n");
        printw("F Remove File            E Edit\n");
        printw("M Move to Directory      R Run\n");
        printw("Q Quit                   S Sort Directory Listing\n");
        printw("------------------------------------------------------\n");
        c= getch(); getch();
        switch (c) {
            case 'q':
                endwin();
                exit(0); /* quit */
            case 'e':
                printw("Edit what?: ") ;    //working
                command = read_string();
				command = "pico " + command ;
                system( command.c_str());
				//folder.clear();
                folder_data.clear();
				file.clear();
				get_new_data = true;
                clear();
                break;
            case 'r':
                printw("Run what?: ");	//Runs the code and waits for any character before exiting
                command = read_string();
                /*
                xyz = fork();   //Not working properly so switched to system which sends errors directly to terminal
                if(xyz == 0){
                    execv(s,(char* const*)(command.c_str()));
                }
                wait(NULL);*/
                system(command.c_str());
                printw("\nEnter any key to exit: ");
                getch();
				//folder.clear();
                folder_data.clear();
				file.clear();
				get_new_data = true;
                clear();
                break;
            case 'c':                   //Working
                printw("Change To?: ");
                command = read_string();
				if(chdir( command.c_str() )==-1){
					if(errno == EACCES){
                        std::cout<<"No accesss"<<std::endl;
                        printw("No access");
                    }
					else if(errno == ENOTDIR){
                        std::cout<<"Not a directory"<<std::endl;
                        printw("Not a directory");
                    }
					std::cout<<errno<<"cannot open directory as specified"<<std::endl;
                    getch();
				}
				//folder.clear();
                folder_data.clear();
				file.clear();
				get_new_data = true;
                clear();
                break;
            case 's' :  //Working according to date and size
                printw("S Sort by Size     D Sort by Date ");
                user = getch();
                getch();
                if(!folder_data.empty())
                switch (user)
                {
                case 's':       //Appears to be having some issues
                    std::sort(folder_data.begin(), folder_data.end(),sort_size);
                    break;
                case 'd':
                    std::sort(folder_data.begin(), folder_data.end(),sort_date);
                    break;
                default:
                    break;
                }
				std::cout<<"Directory is sorted"<<std::endl;
				get_new_data = false;
                clear();
                break;
            case 'm' :
                ;
                break;
            case 'f' :
                ;
                break;
            case 'n':
                print_items(file,start_next_from);  //inwork
                break;
            case 'p':
                break;
        }
    }
    endwin();
    return 0;
}
