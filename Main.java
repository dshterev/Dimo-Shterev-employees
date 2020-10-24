import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {

	public static final String patternYDM = "^(201[0-7]|200[0-9]|[0-1][0-9]{3})(\\/|-|\\.)(1[0-2]|0[1-9])(\\/|-|\\.)(3[01]|[0-2][1-9]|[12]0)$";
	public static final String patternDMY = "^(?:(?:31(\\/|-|\\.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\/|-|\\.)(?:0?[13-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\/|-|\\.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\/|-|\\.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$";

	public static void main(String[] args) {
		Long firstEmpId = null;
		Long secondEmpId = null;
		Long projectId = null;
		int daysTogether = 0;
		Long maxOverlap = null;
		
		List<Employee> employees = new ArrayList<Employee>();
		
		// open chooser and choose file
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Only text files", "txt");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			List<String> allLines;
			try {
				// read all rows in file
				allLines = Files.readAllLines(Paths.get(chooser.getSelectedFile().getAbsolutePath()));
				for (String line : allLines) {
					// convert rows to employee data
					employees.add(getEmployeeFromRow(line));
				}
			} catch (IOException e) {
				System.out.println("Problem with reading the file.");
				e.printStackTrace();
			}

			if (employees.size() == 0) {
				System.out.println("There are no employees in the file.");
				return;
			}

			// find the overlap
			for (int i = 0; i < employees.size() - 1; i++) {
				for (int j = i + 1; j < employees.size(); j++) {
					Employee e1 = employees.get(i);
					Employee e2 = employees.get(j);
					if (e1.getProjectID().equals(e2.getProjectID())) {
						Long overlap = findOverlap(e1, e2);
						if (overlap != null) {
							if (maxOverlap == null) {
								maxOverlap = overlap;
								firstEmpId = e1.getId();
								secondEmpId = e2.getId();
								projectId = e1.getProjectID();
								daysTogether = getDaysFromOverlap(overlap);
							} else {
								if (overlap > maxOverlap) {
									maxOverlap = overlap;
									firstEmpId = e1.getId();
									secondEmpId = e2.getId();
									projectId = e1.getProjectID();
									daysTogether = getDaysFromOverlap(overlap);
								}
							}
						}
					}
				}
			}

			// output
			if (maxOverlap == null) {
				System.out.println("There isn't overlap.");
			} else {
				// Employee ID #1, Employee ID #2, Project ID, Days worked
				System.out.println("Employee ID #1 = " + firstEmpId);
				System.out.println("Employee ID #2 = " + secondEmpId);
				System.out.println("Project ID = " + projectId);
				System.out.println("Days worked = " + daysTogether);
			}
		}
	}

	public static int getDaysFromOverlap(Long overlap) {
		return (int) (overlap / (1000 * 60 * 60 * 24));
	}

	public static Long findOverlap(Employee emp1, Employee emp2) {
		if (emp1.getDateFrom() > emp2.getDateFrom()) {
			Employee tmp = emp1;
			emp1 = emp2;
			emp2 = tmp;
		}
		Long start1 = emp1.getDateFrom();
		Long start2 = emp2.getDateFrom();
		Long end1 = emp1.getDateTo();
		Long end2 = emp2.getDateTo();
		Long totalRange = Math.max(end1, end2) - Math.min(start1, start2);
		Long sumOfRanges = (end1 - start1) + (end2 - start2);
		Long overlappingInterval = null;
		// only if overlap
		if (sumOfRanges > totalRange) {
			overlappingInterval = Math.min(end1, end2) - Math.max(start1, start2);
		}

		return overlappingInterval;
	}

	public static Employee getEmployeeFromRow(String row) {
		String[] arr = row.split(",");
		Employee res = new Employee();
		res.setId(Long.parseLong(arr[0].trim()));
		res.setProjectID(Long.parseLong(arr[1].trim()));
		res.setDateFrom(getDateFromAnyStringFormat(arr[2].trim()));
		res.setDateTo(getDateFromAnyStringFormat(arr[3].trim()));
		return res;
	}

	public static Long getDateFromAnyStringFormat(String dateStr) {
		if ("null".equals(dateStr.trim().toLowerCase())) {
			return System.currentTimeMillis();
		}
		Long res = null;
		Pattern myPatternDMY = Pattern.compile(patternDMY);
		Matcher matcherDMY = myPatternDMY.matcher(dateStr);
		Pattern myPatternYMD = Pattern.compile(patternYDM);
		Matcher matcherYMD = myPatternYMD.matcher(dateStr);
		try {
			if (matcherDMY.matches()) {
				String format = getSimpleFormat(dateStr, true);
				SimpleDateFormat sdf = new SimpleDateFormat(format);
				Date date = sdf.parse(dateStr);
				res = date.getTime();

			} else if (matcherYMD.matches()) {
				String format = getSimpleFormat(dateStr, false);
				SimpleDateFormat sdf = new SimpleDateFormat(format);
				Date date = sdf.parse(dateStr);
				res = date.getTime();

			} else {
				throw new Exception("Invalid date");
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}

	public static String getSimpleFormat(String text, boolean isDMY) {
		String year = "yyyy";
		if (text.length() == 8) {
			year = "yy";
		}
		String month = "MM";
		String day = "dd";
		String res = "";
		Pattern pattern = Pattern.compile("(\\D)");
		Matcher matcher = pattern.matcher(text);
		String splitter = "";
		if (matcher.find()) {
			for (int i = 0; i < matcher.groupCount(); i++) {
				String tmp = matcher.group(i) + "";
				if (tmp.length() == 1) {
					splitter = tmp;
				}
			}
		}
		String[] arr = text.split(splitter);
		if (isDMY) {
			res = day + splitter + month + splitter + year;
		} else {
			res = year + splitter + month + splitter + day;
		}

		return res;
	}
}

class Employee {
	// EmpID, ProjectID, DateFrom, DateTo
	private Long id;
	private Long projectID;
	private Long dateFrom;
	private Long dateTo;

	public Employee() {
	}

	public Employee(Long id, Long projectID, Long dateFrom, Long dateTo) {
		this.id = id;
		this.projectID = projectID;
		this.dateFrom = dateFrom;
		this.dateTo = dateTo;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProjectID() {
		return projectID;
	}

	public void setProjectID(Long projectID) {
		this.projectID = projectID;
	}

	public Long getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(Long dateFrom) {
		this.dateFrom = dateFrom;
	}

	public Long getDateTo() {
		return dateTo;
	}

	public void setDateTo(Long dateTo) {
		this.dateTo = dateTo;
	}
}
