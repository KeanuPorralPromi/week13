package pet.store.service;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pet.store.controller.model.PetStoreData;
import pet.store.controller.model.PetStoreData.PetStoreCustomer;
import pet.store.controller.model.PetStoreData.PetStoreEmployee;
import pet.store.dao.CustomerDao;
import pet.store.dao.EmployeeDao;
import pet.store.dao.PetStoreDao;
import pet.store.entity.Customer;
import pet.store.entity.Employee;
import pet.store.entity.PetStore;

@Service
public class PetStoreService {
	@Autowired
	private PetStoreDao petStoreDao;
	
	@Autowired
	private EmployeeDao employeeDao;

	@Autowired
	private CustomerDao customerDao;
	
	@Transactional(readOnly = false)
	public PetStoreData savePetStore(PetStoreData petStoreData) {
		Long petStoreId = petStoreData.getPetStoreId();
		PetStore petStore = findOrCreatePetStore(petStoreId, petStoreData.getPetStorePhone());
		copyPetStoreFields(petStore, petStoreData);

		return new PetStoreData(petStoreDao.save(petStore));
	}

	private void copyPetStoreFields(PetStore petStore, PetStoreData petStoreData) {
		petStore.setPetStoreId(petStoreData.getPetStoreId());
		;
		petStore.setPetStoreName(petStoreData.getPetStoreName());
		petStore.setPetStoreAddress(petStoreData.getPetStoreAddress());
		petStore.setPetStoreCity(petStoreData.getPetStoreCity());
		petStore.setPetStoreState(petStoreData.getPetStoreState());
		petStore.setPetStoreZip(petStoreData.getPetStoreZip());
		petStore.setPetStorePhone(petStoreData.getPetStorePhone());

	}

	private PetStore findOrCreatePetStore(Long petStoreId, Long petStorePhone) {
		PetStore petStore;

		if (Objects.isNull(petStoreId)) {
			Optional<PetStore> opStorePhone = petStoreDao.findByPetStorePhone(petStorePhone);
			if(opStorePhone.isPresent()) {
				throw new DuplicateKeyException("Pet store with the phone " + petStorePhone + " already Exists");
			}
			petStore = new PetStore();

		} else {
			petStore = findByPetStoreId(petStoreId);
		}

		return petStore;
	}

	private PetStore findByPetStoreId(Long petStoreId) {

		return petStoreDao.findById(petStoreId)
				.orElseThrow(() -> new NoSuchElementException("Pet Store with ID=" + petStoreId + " was not found."));
	}
	@Transactional(readOnly = false)
	public PetStoreEmployee saveEmployee(Long petStoreId, PetStoreEmployee petStoreEmployee) {
		PetStore petStore = findByPetStoreId(petStoreId);
		Long employeeId = petStoreEmployee.getEmployeeId();
		Employee employee = findOrCreateEmployee(employeeId, petStoreId);
		copyEmployeeFields(employee, petStoreEmployee);
		
		employee.setPetStore(petStore);
		petStore.getEmployees().add(employee);
		
		Employee dbEmployee = employeeDao.save(employee);
		
		return new PetStoreEmployee(dbEmployee);
	}
	public Employee findEmployeeById(Long employeeId, Long petStoreId) {
		
		Employee employee = employeeDao.findById(employeeId).orElseThrow(() -> new NoSuchElementException("Employee with ID=" + employeeId + " was not found"));
		
		if(employee.getPetStore().getPetStoreId() == petStoreId) {
			return employee;
		} else {
			throw new IllegalArgumentException();
		}
	}
	public Employee findOrCreateEmployee(Long employeeId, Long petStoreId) {
		Employee employee;
		
		if(Objects.isNull(employeeId)) {
			employee = new Employee();
		} else {
			employee = findEmployeeById(employeeId, petStoreId);
		}
		return employee;
	}
	public void copyEmployeeFields(Employee employee, PetStoreEmployee petStoreEmployee) {
		employee.setEmployeeId(petStoreEmployee.getEmployeeId());
		employee.setEmployeeFirstName(petStoreEmployee.getEmployeeFirstName());
		employee.setEmployeeLastName(petStoreEmployee.getEmployeeLastName());
		employee.setEmployeePhone(petStoreEmployee.getEmployeePhone());
		employee.setEmployeeJobTitle(petStoreEmployee.getEmployeeJobTitle());
		
		
	}
	@Transactional(readOnly = false)
	public PetStoreCustomer saveCustomer(Long petStoreId, PetStoreCustomer petStoreCustomer) {
		PetStore petStore = findByPetStoreId(petStoreId);
		Long customerId = petStoreCustomer.getCustomerId();
		Customer customer = findOrCreateCustomer(customerId, petStoreId);
		copyCustomerFields(customer, petStoreCustomer);
		
		
		for(PetStore pt : customer.getPetStores()) {
			pt.getCustomers().add(customer);
			customer.getPetStores().add(pt);
		}
		petStore.getCustomers().add(customer);
		
		Customer dbCustomer = customerDao.save(customer);
		return new PetStoreCustomer(dbCustomer);
	}

	private void copyCustomerFields(Customer customer, PetStoreCustomer petStoreCustomer) {
	
		customer.setCustomerId(petStoreCustomer.getCustomerId());
		customer.setCustomerFirstName(petStoreCustomer.getCustomerFirstName());
		customer.setCustomerLastName(petStoreCustomer.getCustomerLastName());
		customer.setCustomerEmail(petStoreCustomer.getCustomerEmail());

		
		
	}

	private Customer findOrCreateCustomer(Long customerId, Long petStoreId) {
		Customer customer;
		
		if(Objects.isNull(customerId)) {
			customer = new Customer();
		} else {
			customer = findByCustomerId(customerId, petStoreId);
		}
		
		return customer;
	}

	private Customer findByCustomerId(Long customerId, Long petStoreId) {
		
		Customer customer = customerDao.findById(customerId).orElseThrow(() -> new NoSuchElementException("Customer with the ID=" + customerId + " does not exists"));
		Customer correctCustomer = new Customer(); 
		for(PetStore petStore : customer.getPetStores()) {
			if(petStore.getPetStoreId() == petStoreId) {
				correctCustomer = customer;
			} else {
				throw new IllegalArgumentException();
			}
			
		}
		return correctCustomer;	
	}
	@Transactional(readOnly = true)
	public List	<PetStoreData> retrieveAllPetStores() {
		List<PetStore> ps = petStoreDao.findAll(); 
		List<PetStoreData> result = new LinkedList<>();
		
		for(PetStore petStore : ps) {
			PetStoreData psd = new PetStoreData(petStore);
			psd.getCustomers().clear();
			psd.getEmployees().clear();
			
			result.add(psd);
		}
		return result;
	}
	@Transactional(readOnly = true)
	public PetStoreData retrievePetStoreById(Long petStoreId) {
		
		PetStoreData psd = new PetStoreData(findByPetStoreId(petStoreId));
		return psd;
	}
	@Transactional(readOnly = false)
	public void deletePetStoreById(Long petStoreId) {
		PetStore ps = findByPetStoreId(petStoreId);
		petStoreDao.delete(ps);
		
		
	}
	
}


