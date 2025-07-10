package gg.nextforge.utility;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ChatPagination {

    int currentPage;
    int totalPages;
    int linesPerPage;
    List<String> lines;

    public ChatPagination(int linesPerPage) {
        this.lines = new ArrayList<>();
        this.linesPerPage = linesPerPage;
        this.totalPages = 0;
        this.currentPage = 0;
    }

    public void addLine(String line) {
        lines.add(line);
        totalPages = (int) Math.ceil((double) lines.size() / linesPerPage);
    }

    public List<String> getPage(int page) {
        if (page < 0 || page >= totalPages) {
            throw new IndexOutOfBoundsException("Page number out of range");
        }
        int start = page * linesPerPage;
        int end = Math.min(start + linesPerPage, lines.size());
        return lines.subList(start, end);
    }

}
